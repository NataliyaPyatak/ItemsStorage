package ru.npyatak.itemsStorage.controllers;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import ru.npyatak.itemsStorage.entities.Item;
import ru.npyatak.itemsStorage.repositories.ItemRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 *
 *
 * @author natalapatak
 * @since 12.09.2026
 */
@RestController
public class WebhookController
{

    private final ItemRepository repo;
    private final ObjectMapper mapper;
    @Value("${skill.id}")
    private String expectedSkillId;

    public WebhookController(ItemRepository repo, ObjectMapper mapper)
    {
        this.repo = repo;
        this.mapper = mapper;
    }

    @PostMapping("/")
    public JsonNode webhook(@RequestBody JsonNode request)
    {
        try
        {
            return processRequest(request);
        }
        catch (Exception e)
        {
            // Алиса всегда ждёт корректный ответ — вместо 500 отдаём сообщение об ошибке
            return buildResponse(request, "Извините, произошла ошибка. Попробуйте ещё раз.", false, "");
        }
    }

    private JsonNode processRequest(JsonNode request)
    {
        String skillId = request.path("session").path("skill_id").asText();
        if(!skillId.equals(expectedSkillId))
        {
            // Чужой запрос — молча игнорируем
            ObjectNode ignored = mapper.createObjectNode();
            ObjectNode resp = ignored.putObject("response");
            resp.put("text", "");
            resp.put("end_session", true);
            ignored.put("version", "1.0");
            return ignored;
        }
        String command = request.path("request").path("command").asText().toLowerCase().trim();
        String userId = request.path("session").path("user").path("user_id").asText();
        boolean isNewSession = request.path("session").path("new").asBoolean(false);
        // Текущее хранилище из state (если уже выбрано)
        String currentStorage = request.path("state").path("session").path("storage").asText("");

        // Проверяем, не хочет ли пользователь выйти
        boolean endSession = command.matches(".*(выйти|выход|пока|хватит|отстань|закончить|стоп|до свидания).*");

        String responseText;
        String newStorage = currentStorage;  // сохраняем текущее, если не меняем
        if (userId.isBlank())
        {
            return buildResponse(request, "Чтобы пользоваться навыком, войдите в аккаунт Яндекса.", true, "");
        }

        if (endSession)
        {
            responseText = "Пока! Возвращайся, когда что-то понадобится.";
        }
        else if (isNewSession || currentStorage.isBlank())
        {
            // Старт: спрашиваем хранилище
            if (command.isBlank() || isNewSession)
            {
                // Показываем известные хранилища, если есть
                List<String> listStorages = repo.findDistinctStoragesByUserId(userId);
                if (listStorages.isEmpty())
                {
                    responseText = "Привет! Назови хранилище, например: квартира, дача, гараж.";
                }
                else
                {
                    responseText = "Привет! Какое хранилище? Известные: "
                            + String.join(", ", listStorages)
                            + ". Или назови новое.";
                }
            }
            else
            {
                // Пользователь назвал хранилище
                newStorage = command;
                responseText = "Окей, хранилище: " + command
                        + ". Теперь спрашивай, где что лежит.";
            }
        }
        else if (command.matches("переключи .+|смени хранилище .+"))
        {
            // Команда переключения хранилища
            newStorage = command
                    .replaceFirst("(переключи\\s+(?:на\\s+)?|смени\\s+хранилище\\s+(?:на\\s+)?)", "")
                    .trim();
            responseText = "Переключила на " + newStorage + ".";
        }
        else
        {
            responseText = processCommand(command, userId, newStorage);
        }

        return buildResponse(request, responseText, endSession, newStorage);
    }

    /**
     * Собирает ответ для Алисы через Jackson — экранирование выполняется автоматически.
     */
    private JsonNode buildResponse(JsonNode request, String text, boolean endSession, String storage)
    {
        ObjectNode root = mapper.createObjectNode();
        ObjectNode response = root.putObject("response");
        response.put("text", text);
        response.put("end_session", endSession);
        root.put("version", "1.0");
        JsonNode session = request.path("session");
        if (session.isObject())
        {
            root.set("session", session.deepCopy());
        }
        else
        {
            root.putObject("session");
        }
        ObjectNode sessionState = root.putObject("session_state");
        sessionState.put("storage", storage);
        return root;
    }

    private String processCommand(String command, String userId, String storage)
    {

        // --- "где шуруповёрт" ---
        if (command.startsWith("где "))
        {
            String thing = command.substring(4).trim();
            List<Item> found = repo.findByNameIgnoreCaseContainingAndUserIdAndStorage(thing, userId, storage);
            if (found.isEmpty())
            {
                return "Не нашла " + thing + " в " + storage + ". Может, под другим названием?";
            }
            StringBuilder sb = new StringBuilder();
            for (Item item : found)
            {
                sb.append(item.getName())
                        .append(" — ")
                        .append(item.getLocation());
                if (item.getNote() != null && !item.getNote().isBlank())
                {
                    sb.append(" (").append(item.getNote()).append(")");
                }
                sb.append(". ");
            }
            return sb.toString().trim();
        }

        // --- "положил шуруповёрт в ящик 3" ---
        if (command.startsWith("положил ") || command.startsWith("переложил "))
        {
            Pattern p = Pattern.compile("(?:положил|переложил)\\s+(.+?)\\s+в\\s+(.+)");
            Matcher m = p.matcher(command);
            if (!m.matches())
            {
                return "Не поняла формат. Скажи: «положил шуруповёрт в ящик 3».";
            }
            String thing = m.group(1).trim();
            String place = m.group(2).trim();

            Item existing = repo.findByNameIgnoreCaseAndUserIdAndStorage(thing, userId, storage);
            if (existing != null)
            {
                existing.setLocation(place);
                repo.save(existing);
                return "Записала: " + existing.getName() + " теперь в " + place + ".";
            }
            else
            {
                repo.save(new Item(thing, place, "", userId, storage));
                return "Добавила: " + thing + " в " + place + ".";
            }
        }

        // --- "что в ящике 1" ---
        if (command.startsWith("что в "))
        {
            String place = command.substring(6).trim();
            List<Item> items = repo.findByLocationIgnoreCaseContainingAndUserIdAndStorage(place, userId, storage);
            if (items.isEmpty())
            {
                return "В " + place + " ничего не записано.";
            }
            String names = items.stream()
                    .map(Item::getName)
                    .collect(Collectors.joining(", "));
            return "В " + place + ": " + names + ".";
        }

        // --- "добавь молоток в ящик 2" ---
        if (command.startsWith("добавь "))
        {
            Pattern p = Pattern.compile("добавь\\s+(.+?)\\s+в\\s+(.+)");
            Matcher m = p.matcher(command);
            if (!m.matches())
            {
                return "Не поняла. Скажи: «добавь молоток в ящик 2».";
            }
            String thing = m.group(1).trim();
            String place = m.group(2).trim();
            repo.save(new Item(thing, place, "", userId, storage));
            return "Добавила: " + thing + " в " + place + ".";
        }

        // --- "удали изоленту" ---
        if (command.startsWith("удали "))
        {
            String thing = command.substring(6).trim();
            Item existing = repo.findByNameIgnoreCaseAndUserIdAndStorage(thing, userId, storage);
            if (existing != null)
            {
                repo.delete(existing);
                return "Удалила " + thing + " из базы.";
            }
            return "Не нашла " + thing + ".";
        }

        // справка
        return "Я умею: «где шуруповёрт», «что в ящике 1», "
                + "«положил шуруповёрт в ящик 3», «добавь молоток в ящик 2», "
                + "«удали изоленту», «переключи на дачу», «выход».";
    }
}

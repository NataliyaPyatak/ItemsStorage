package ru.npyatak.itemsStorage.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import ru.npyatak.itemsStorage.entities.Item;
import ru.npyatak.itemsStorage.parser.IntentParser;
import ru.npyatak.itemsStorage.parser.ParsedCommand;
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

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    /** Справка для нераспознанных фраз */
    private static final String HELP = "Я умею: «где шуруповёрт», «что в ящике 3», "
            + "«положила дрель в ящик 1», «запиши, что дрель теперь в ящике 1», "
            + "«добавь молоток в ящик 2», «удали изоленту», «переключи на дачу», «выход».";

    private final ItemRepository repo;
    private final ObjectMapper mapper;
    private final IntentParser parser;
    @Value("${skill.id}")
    private String expectedSkillId;

    public WebhookController(ItemRepository repo, ObjectMapper mapper, IntentParser parser)
    {
        this.repo = repo;
        this.mapper = mapper;
        this.parser = parser;
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
        ParsedCommand parsed = parser.parse(command);
        log.info("фраза: «{}» → intent: {}, item: «{}», place: «{}» (storage: «{}», user: {})",
                command, parsed.intent(), parsed.item(), parsed.place(), storage, userId);
        return switch (parsed.intent())
        {
            case PUT -> put(parsed, userId, storage);
            case ADD -> add(parsed, userId, storage);
            case FIND -> find(parsed, userId, storage);
            case WHAT_IN -> whatIn(parsed, userId, storage);
            case DELETE -> delete(parsed, userId, storage);
            case UNKNOWN -> HELP;
        };
    }

    /** Где вещь: сравниваем основы слов, падеж не важен */
    private String find(ParsedCommand parsed, String userId, String storage)
    {
        List<Item> found = repo.findByUserIdAndStorage(userId, storage).stream()
                .filter(item -> parser.matchesByStem(parsed.item(), item.getName()))
                .toList();
        if (found.isEmpty())
        {
            return "Не нашла " + parsed.item() + " в " + storage + ". Может, под другим названием?";
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

    /** Что лежит в месте: сравниваем места по основам */
    private String whatIn(ParsedCommand parsed, String userId, String storage)
    {
        String place = parsed.place();
        String names = repo.findByUserIdAndStorage(userId, storage).stream()
                .filter(item -> parser.matchesByStem(place, item.getLocation()))
                .map(Item::getName)
                .collect(Collectors.joining(", "));
        if (names.isEmpty())
        {
            return "В " + place + " ничего не записано.";
        }
        return "В " + place + ": " + names + ".";
    }

    /** Положил/переложил/запиши: обновляем место существующей вещи, иначе создаём */
    private String put(ParsedCommand parsed, String userId, String storage)
    {
        if (parsed.place().isBlank())
        {
            return "Не поняла, куда положить. Скажи: «положила дрель в ящик 1».";
        }
        Item existing = findByName(parsed.item(), userId, storage);
        if (existing != null)
        {
            existing.setLocation(parsed.place());
            repo.save(existing);
            return "Записала: " + existing.getName() + " теперь в " + parsed.place() + ".";
        }
        repo.save(new Item(parsed.item(), parsed.place(), "", userId, storage));
        return "Добавила: " + parsed.item() + " в " + parsed.place() + ".";
    }

    /** Добавь: всегда создаёт новую запись, даже если вещь уже записана */
    private String add(ParsedCommand parsed, String userId, String storage)
    {
        if (parsed.place().isBlank())
        {
            return "Не поняла, куда добавить. Скажи: «добавь молоток в ящик 2».";
        }
        repo.save(new Item(parsed.item(), parsed.place(), "", userId, storage));
        return "Добавила: " + parsed.item() + " в " + parsed.place() + ".";
    }

    /** Удали: винительный падеж уже сведён к именительному, ищем по основам */
    private String delete(ParsedCommand parsed, String userId, String storage)
    {
        Item existing = findByName(parsed.item(), userId, storage);
        if (existing != null)
        {
            repo.delete(existing);
            return "Удалила " + existing.getName() + " из базы.";
        }
        return "Не нашла " + parsed.item() + ".";
    }

    /** Ищет вещь среди хранилища, сравнивая основы слов */
    private Item findByName(String name, String userId, String storage)
    {
        for (Item item : repo.findByUserIdAndStorage(userId, storage))
        {
            if (parser.matchesByStem(name, item.getName()))
            {
                return item;
            }
        }
        return null;
    }
}

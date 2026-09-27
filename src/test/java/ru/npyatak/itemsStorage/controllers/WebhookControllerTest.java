package ru.npyatak.itemsStorage.controllers;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import ru.npyatak.itemsStorage.entities.Item;
import ru.npyatak.itemsStorage.parser.IntentParser;
import ru.npyatak.itemsStorage.repositories.ItemRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

/**
 * Тесты разбора команд через IntentParser: падежи, женский род глагола,
 * «запиши, что … теперь …». Репозиторий мокается — контроллер проверяем в отрыве от БД.
 *
 * @author natalapatak
 * @since 27.09.2026
 */
class WebhookControllerTest
{

    private static final String USER_ID = "user1";
    private static final String STORAGE = "квартира";

    private ItemRepository repo;
    private ObjectMapper mapper;
    private WebhookController controller;

    @BeforeEach
    void setUp()
    {
        repo = Mockito.mock(ItemRepository.class);
        mapper = new ObjectMapper();
        controller = new WebhookController(repo, mapper, new IntentParser());
        ReflectionTestUtils.setField(controller, "expectedSkillId", "test-skill");
    }

    // --- FIND: «где отвёртки» находит сохранённую «отвёртка» ---

    @Test
    void findFindsItemByCaseForm()
    {
        when(repo.findByUserIdAndStorage(USER_ID, STORAGE))
                .thenReturn(List.of(new Item("отвёртка", "ящик 2", "", USER_ID, STORAGE)));

        String text = responseText(controller.webhook(request("где отвёртки")));

        assertThat(text).contains("отвёртка — ящик 2");
    }

    // --- WHAT_IN: «что в ящике 3» (предложный) находит место «ящик 3» ---

    @Test
    void whatInMatchesPredlozhnyCase()
    {
        when(repo.findByUserIdAndStorage(USER_ID, STORAGE))
                .thenReturn(List.of(new Item("молоток", "ящик 3", "", USER_ID, STORAGE)));

        String text = responseText(controller.webhook(request("что в ящике 3")));

        assertThat(text).contains("молоток");
    }

    // --- PUT: женский род глагола, обновление существующей вещи ---

    @Test
    void putFeminineUpdatesExisting()
    {
        when(repo.findByUserIdAndStorage(USER_ID, STORAGE))
                .thenReturn(List.of(new Item("дрель", "ящик 1", "", USER_ID, STORAGE)));

        String text = responseText(controller.webhook(request("положила дрель в ящик 2")));

        assertThat(text).isEqualTo("Записала: дрель теперь в ящик 2.");
        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getLocation()).isEqualTo("ящик 2");
    }

    // --- PUT: «запиши, что … теперь …» ---

    @Test
    void zapishiChtoUpdatesExisting()
    {
        when(repo.findByUserIdAndStorage(USER_ID, STORAGE))
                .thenReturn(List.of(new Item("дрель", "гараж", "", USER_ID, STORAGE)));

        String text = responseText(controller.webhook(request("запиши, что дрель теперь в ящике 1")));

        assertThat(text).contains("Записала");
        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getLocation()).isEqualTo("ящик 1");
    }

    // --- PUT/ADD: в БД пишется именительный падеж, ответ повторяет сказанное ---

    @Test
    void putStoresNominativeItemAndPlace()
    {
        when(repo.findByUserIdAndStorage(USER_ID, STORAGE)).thenReturn(List.of());

        String text = responseText(controller.webhook(request("положила кружку в сумку")));

        assertThat(text).isEqualTo("Добавила: кружка в сумку.");
        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("кружка");
        assertThat(captor.getValue().getLocation()).isEqualTo("сумка");
    }

    @Test
    void findAnswersWithStoredNominativeForms()
    {
        when(repo.findByUserIdAndStorage(USER_ID, STORAGE))
                .thenReturn(List.of(new Item("кружка", "сумка", "", USER_ID, STORAGE)));

        String text = responseText(controller.webhook(request("где кружка")));

        assertThat(text).isEqualTo("кружка — сумка.");
    }

    @Test
    void addStoresNominativePlace()
    {
        when(repo.findByUserIdAndStorage(USER_ID, STORAGE)).thenReturn(List.of());

        String text = responseText(controller.webhook(request("добавь тарелку в коробку")));

        assertThat(text).isEqualTo("Добавила: тарелка в коробку.");
        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("тарелка");
        assertThat(captor.getValue().getLocation()).isEqualTo("коробка");
    }

    // --- DELETE: «удали изоленту» находит сохранённую «изолента» ---

    @Test
    void deleteByCaseForm()
    {
        when(repo.findByUserIdAndStorage(USER_ID, STORAGE))
                .thenReturn(List.of(new Item("изолента", "ящик 1", "", USER_ID, STORAGE)));

        String text = responseText(controller.webhook(request("удали изоленту")));

        assertThat(text).contains("Удалила");
        verify(repo).delete(any(Item.class));
    }

    // --- ADD: добавляет новую запись, даже если вещь уже есть ---

    @Test
    void addCreatesEvenIfItemExists()
    {
        when(repo.findByUserIdAndStorage(USER_ID, STORAGE))
                .thenReturn(List.of(new Item("молоток", "ящик 5", "", USER_ID, STORAGE)));

        String text = responseText(controller.webhook(request("добавь молоток в ящик 2")));

        assertThat(text).contains("Добавила");
        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getLocation()).isEqualTo("ящик 2");
    }

    // --- UNKNOWN: справка с новыми примерами фраз ---

    @Test
    void unknownShowsUpdatedHelp()
    {
        String text = responseText(controller.webhook(request("привет")));

        assertThat(text).contains("положила дрель в ящик 1");
    }

    /** Собирает запрос Алисы: не новая сессия, хранилище уже выбрано */
    private JsonNode request(String command)
    {
        String json = """
                {
                  "session": {"skill_id": "test-skill", "new": false, "user": {"user_id": "%s"}},
                  "request": {"command": "%s"},
                  "state": {"session": {"storage": "%s"}}
                }
                """.formatted(USER_ID, command, STORAGE);
        return mapper.readTree(json);
    }

    private String responseText(JsonNode response)
    {
        return response.path("response").path("text").asText();
    }
}

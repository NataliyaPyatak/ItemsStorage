package ru.npyatak.itemsStorage.parser;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты разбора «человеческих» фраз: род глагола, падежи, «запиши что», «теперь».
 *
 * @author natalapatak
 * @since 27.09.2026
 */
class IntentParserTest
{

    private final IntentParser parser = new IntentParser();

    // --- PUT: все формы глагола ---

    @Test
    void parsePutMasculineVerb()
    {
        ParsedCommand c = parser.parse("положил шуруповёрт в ящик 3");
        assertThat(c.intent()).isEqualTo(Intent.PUT);
        assertThat(c.item()).isEqualTo("шуруповёрт");
        assertThat(c.place()).isEqualTo("ящик 3");
    }

    @Test
    void parsePutFeminineVerb()
    {
        ParsedCommand c = parser.parse("положила дрель в ящик 1");
        assertThat(c.intent()).isEqualTo(Intent.PUT);
        assertThat(c.item()).isEqualTo("дрель");
        assertThat(c.place()).isEqualTo("ящик 1");
    }

    @Test
    void parsePutPerekladilaOnPolku()
    {
        ParsedCommand c = parser.parse("переложила шуруповёрт на полку");
        assertThat(c.intent()).isEqualTo(Intent.PUT);
        assertThat(c.item()).isEqualTo("шуруповёрт");
        assertThat(c.place()).isEqualTo("полку");
    }

    @Test
    void parsePutUbralaVShkaf()
    {
        ParsedCommand c = parser.parse("убрала молоток в шкаф");
        assertThat(c.intent()).isEqualTo(Intent.PUT);
        assertThat(c.item()).isEqualTo("молоток");
        assertThat(c.place()).isEqualTo("шкаф");
    }

    @Test
    void parsePutZapishiChtoTeper()
    {
        ParsedCommand c = parser.parse("запиши, что дрель теперь в ящике 1");
        assertThat(c.intent()).isEqualTo(Intent.PUT);
        assertThat(c.item()).isEqualTo("дрель");
        assertThat(c.place()).isEqualTo("ящике 1");
    }

    @Test
    void parsePutZapishiChtoTeperPeredVeshju()
    {
        ParsedCommand c = parser.parse("запиши, что теперь молоток в шкафу");
        assertThat(c.intent()).isEqualTo(Intent.PUT);
        assertThat(c.item()).isEqualTo("молоток");
        assertThat(c.place()).isEqualTo("шкафу");
    }

    // --- WHAT_IN: предложный и именительный падежи ---

    @Test
    void parseWhatInPredlozhny()
    {
        ParsedCommand c = parser.parse("что в ящике 3");
        assertThat(c.intent()).isEqualTo(Intent.WHAT_IN);
        assertThat(c.place()).isEqualTo("ящике 3");
    }

    @Test
    void parseWhatInLieshit()
    {
        ParsedCommand c = parser.parse("что лежит в ящик 3");
        assertThat(c.intent()).isEqualTo(Intent.WHAT_IN);
        assertThat(c.place()).isEqualTo("ящик 3");
    }

    // --- FIND ---

    @Test
    void parseGde()
    {
        ParsedCommand c = parser.parse("где шуруповёрт");
        assertThat(c.intent()).isEqualTo(Intent.FIND);
        assertThat(c.item()).isEqualTo("шуруповёрт");
    }

    @Test
    void parseGdeGenitiveWithQuestionMark()
    {
        ParsedCommand c = parser.parse("где отвёртки?");
        assertThat(c.intent()).isEqualTo(Intent.FIND);
        assertThat(c.item()).isEqualTo("отвёртки");
    }

    @Test
    void parseNajdi()
    {
        ParsedCommand c = parser.parse("найди дрель");
        assertThat(c.intent()).isEqualTo(Intent.FIND);
        assertThat(c.item()).isEqualTo("дрель");
    }

    // --- ADD / DELETE ---

    @Test
    void parseDobav()
    {
        ParsedCommand c = parser.parse("добавь молоток в ящик 2");
        assertThat(c.intent()).isEqualTo(Intent.ADD);
        assertThat(c.item()).isEqualTo("молоток");
        assertThat(c.place()).isEqualTo("ящик 2");
    }

    @Test
    void parseUdali()
    {
        ParsedCommand c = parser.parse("удали изоленту");
        assertThat(c.intent()).isEqualTo(Intent.DELETE);
        assertThat(c.item()).isEqualTo("изолента");
    }

    // --- UNKNOWN ---

    @Test
    void parseUnknownPhrase()
    {
        assertThat(parser.parse("привет").intent()).isEqualTo(Intent.UNKNOWN);
    }

    // --- стеммер: одинаковые основы у разных форм ---

    @Test
    void stemMatchesPredlozhnyAndNominative()
    {
        assertThat(parser.stem("ящике")).isEqualTo(parser.stem("ящик"));
    }

    @Test
    void stemMatchesGenitivePluralOfItem()
    {
        assertThat(parser.stem("отвёртки")).isEqualTo(parser.stem("отвёртка"));
    }

    @Test
    void stemMatchesInstrumentalCase()
    {
        assertThat(parser.stem("дрелью")).isEqualTo(parser.stem("дрель"));
    }

    // --- сверка по стемам для поиска в БД ---

    @Test
    void matchesByStemCaseForms()
    {
        assertThat(parser.matchesByStem("ящике 3", "ящик 3")).isTrue();
        assertThat(parser.matchesByStem("отвёртки", "отвёртка")).isTrue();
        assertThat(parser.matchesByStem("дрелью", "дрель")).isTrue();
    }

    @Test
    void matchesByStemDifferentWords()
    {
        assertThat(parser.matchesByStem("ящик 3", "полка")).isFalse();
        assertThat(parser.matchesByStem("дрель", "шуруповёрт")).isFalse();
    }
}

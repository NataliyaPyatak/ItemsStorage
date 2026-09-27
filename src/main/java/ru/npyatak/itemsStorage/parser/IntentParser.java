package ru.npyatak.itemsStorage.parser;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Разбор «человеческой» фразы в команду: намерение + вещь + место.
 * Намерение ищем по основе глагола (положил/положила/положить...), сущности
 * разделяем предлогом, а формы слов (падежи) сравниваем лёгким стеммером.
 *
 * @author natalapatak
 * @since 27.09.2026
 */
@Component
public class IntentParser
{
    /** Окончания для усечения (сначала длинные). «ь»/«й» не трогаем: дрель, музей */
    private static final String[] ENDINGS = {
            "ами", "ями", "ого", "его", "ому", "ему", "ыми", "ими",
            "ой", "ый", "ий", "ая", "яя", "ое", "ее", "ые", "ие",
            "ом", "ем", "ах", "ях", "ам", "ям", "ов", "ев", "ей", "ую", "ых", "их",
            "у", "ю", "а", "я", "о", "е", "ы", "и"
    };

    /** Основы глаголов «положить вещь»: положил/положила/положить, переложила, убрала */
    private static final String[] PUT_VERBS = {"положи", "переложи", "убра"};

    /** Основа глагола «добавить»: добавь/добавил/добавила */
    private static final String ADD_VERB = "добав";

    /** Основа глагола «удалить»: удали/удалить/удалила */
    private static final String DELETE_VERB = "удал";

    /** Ключевые слова поиска: где/найди/найти */
    private static final String[] FIND_KEYS = {"где", "найди", "найти"};

    /** «вещь в/на/под/за место» — предлог разделяет сущности */
    private static final Pattern PLACE_PATTERN = Pattern.compile(
            "^(.+?)\\s+(?:в|во|на|под|за)\\s+(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /** «что (лежит) в место» */
    private static final Pattern WHAT_IN_PATTERN = Pattern.compile(
            "^что\\s+(?:лежит\\s+)?(?:в|во)\\s+(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public ParsedCommand parse(String command)
    {
        if (command == null || command.isBlank())
        {
            return new ParsedCommand(Intent.UNKNOWN, "", "");
        }
        String text = command.trim();
        String normalized = normalize(text);

        // «запиши, что дрель теперь в ящике 1» — синоним «положил»
        if (normalized.startsWith("запиши"))
        {
            String body = text.replaceFirst("(?i)^запиши\\s*,?\\s*(?:что\\s+)?", "");
            return withEntities(Intent.PUT, removeTeper(body));
        }
        for (String verb : PUT_VERBS)
        {
            if (normalized.startsWith(verb))
            {
                return withEntities(Intent.PUT, stripFirstWord(text));
            }
        }
        if (normalized.startsWith(ADD_VERB))
        {
            return withEntities(Intent.ADD, stripFirstWord(text));
        }
        if (normalized.startsWith(DELETE_VERB))
        {
            return new ParsedCommand(Intent.DELETE, stripAccusative(clean(stripFirstWord(text))), "");
        }
        for (String key : FIND_KEYS)
        {
            if (normalized.startsWith(key))
            {
                return new ParsedCommand(Intent.FIND, clean(stripFirstWord(text)), "");
            }
        }
        Matcher whatIn = WHAT_IN_PATTERN.matcher(text);
        if (whatIn.matches())
        {
            return new ParsedCommand(Intent.WHAT_IN, "", clean(whatIn.group(1)));
        }
        return new ParsedCommand(Intent.UNKNOWN, "", "");
    }

    public String stem(String word)
    {
        if (word == null)
        {
            return "";
        }
        String w = normalize(word);
        if (w.length() <= 3)
        {
            return w;
        }
        for (String ending : ENDINGS)
        {
            if (w.endsWith(ending) && w.length() - ending.length() >= 3)
            {
                return w.substring(0, w.length() - ending.length());
            }
        }
        return w;
    }

    public boolean matchesByStem(String query, String stored)
    {
        if (query == null || stored == null)
        {
            return false;
        }
        String[] q = tokenize(query);
        String[] s = tokenize(stored);
        if (q.length != s.length || q.length == 0)
        {
            return false;
        }
        for (int i = 0; i < q.length; i++)
        {
            if (!stem(q[i]).equals(stem(s[i])))
            {
                return false;
            }
        }
        return true;
    }

    /** Разделяет «вещь в место» по предлогу; без предлога место пустое */
    private ParsedCommand withEntities(Intent intent, String body)
    {
        Matcher m = PLACE_PATTERN.matcher(body.trim());
        if (m.matches())
        {
            return new ParsedCommand(intent, clean(m.group(1)), clean(m.group(2)));
        }
        return new ParsedCommand(intent, clean(body), "");
    }

    /** Нижний регистр + ё→е: единая форма для сравнения */
    private String normalize(String text)
    {
        return text.toLowerCase().replace('ё', 'е').trim();
    }

    /** Отбрасывает первое слово (глагол/ключевое слово) */
    private String stripFirstWord(String text)
    {
        int i = text.indexOf(' ');
        return i < 0 ? "" : text.substring(i + 1).trim();
    }

    /** Убирает хвостовую пунктуацию: «отвёртки?» → «отвёртки» */
    private String clean(String text)
    {
        return text.replaceAll("[?!.,;:]+$", "").trim();
    }

    /** «теперь» в любой позиции не влияет на разбор */
    private String removeTeper(String text)
    {
        return text.replaceFirst("(?i)^теперь\\s+", "")
                .replaceFirst("(?i)\\s+теперь(?=\\s|$)", "");
    }

    /** Винительный падеж женского рода: «изоленту» → «изолента» */
    private String stripAccusative(String item)
    {
        if (item.length() >= 5 && item.endsWith("у"))
        {
            return item.substring(0, item.length() - 1) + "а";
        }
        if (item.length() >= 5 && item.endsWith("ю"))
        {
            return item.substring(0, item.length() - 1) + "я";
        }
        return item;
    }

    /** Разбивает фразу на слова, оставляя буквы и цифры */
    private String[] tokenize(String text)
    {
        String[] parts = normalize(text).split("[^\\p{L}\\p{Nd}]+");
        List<String> words = new ArrayList<>();
        for (String part : parts)
        {
            if (!part.isEmpty())
            {
                words.add(part);
            }
        }
        return words.toArray(new String[0]);
    }
}

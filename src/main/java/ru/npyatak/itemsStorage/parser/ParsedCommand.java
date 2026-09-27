package ru.npyatak.itemsStorage.parser;

/**
 * Результат разбора фразы: намерение и извлечённые сущности.
 * Для FIND значим item, для WHAT_IN — place, для UNKNOWN оба пустые.
 *
 * @param intent           намерение
 * @param item             вещь (именительный падеж для PUT/ADD, как сказала — для FIND/DELETE)
 * @param place            место (как сказала пользователь — для ответов)
 * @param placeNominative  место в именительном падеже для хранения; пусто, если не PUT/ADD
 *
 * @author natalapatak
 * @since 27.09.2026
 */
public record ParsedCommand(Intent intent, String item, String place, String placeNominative)
{
    /** Без места для хранения: FIND, WHAT_IN, DELETE, UNKNOWN */
    public ParsedCommand(Intent intent, String item, String place)
    {
        this(intent, item, place, "");
    }
}

package ru.npyatak.itemsStorage.parser;

/**
 * Результат разбора фразы: намерение и извлечённые сущности.
 * Для FIND значим item, для WHAT_IN — place, для UNKNOWN оба пустые.
 *
 * @param intent намерение
 * @param item   вещь (как сказала пользователь)
 * @param place  место (как сказала пользователь)
 *
 * @author natalapatak
 * @since 27.09.2026
 */
public record ParsedCommand(Intent intent, String item, String place)
{
}

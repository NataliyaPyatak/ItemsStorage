package ru.npyatak.itemsStorage.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.npyatak.itemsStorage.entities.Item;

/**
 *
 *
 * @author natalapatak
 * @since 12.09.2026
 */
public interface ItemRepository extends JpaRepository<Item, Long>
{

    // Поиск по точному или частичному совпадению названия (без учёта регистра)
    List<Item> findByNameIgnoreCaseContaining(String name);

    // Поиск по точному совпадению названия
    Item findByNameIgnoreCase(String name);

    // Что лежит в конкретном месте
    List<Item> findByLocationIgnoreCaseContaining(String location);
}

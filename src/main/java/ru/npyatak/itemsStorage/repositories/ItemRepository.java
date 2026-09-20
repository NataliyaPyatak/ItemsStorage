package ru.npyatak.itemsStorage.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ru.npyatak.itemsStorage.entities.Item;

/**
 *
 *
 * @author natalapatak
 * @since 12.09.2026
 */
public interface ItemRepository extends JpaRepository<Item, Long>
{

    List<Item> findByNameIgnoreCaseContainingAndUserIdAndStorage(
            String name, String userId, String storage);

    Item findByNameIgnoreCaseAndUserIdAndStorage(
            String name, String userId, String storage);

    List<Item> findByLocationIgnoreCaseContainingAndUserIdAndStorage(
            String location, String userId, String storage);

    List<Item> findByUserIdAndStorage(String userId, String storage);

    @Query("SELECT DISTINCT i.storage FROM Item i WHERE i.userId = :userId")
    List<String> findDistinctStoragesByUserId(@Param("userId") String userId);
}

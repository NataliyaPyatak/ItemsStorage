package ru.npyatak.itemsStorage.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Вещь
 *
 * @author natalapatak
 * @since 12.09.2026
 */
@Entity
public class Item
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;      // "шуруповёрт"
    private String location;  // "ящик №1"
    private String note;
    private String userId;
    private String storage;

    public Item()
    {
    }

    public Item(String name, String location, String note, String userId, String storage)
    {
        this.name = name;
        this.location = location;
        this.note = note;
        this.userId = userId;
        this.storage = storage;
    }

    public Long getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getLocation()
    {
        return location;
    }

    public String getNote()
    {
        return note;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    public void setNote(String note)
    {
        this.note = note;
    }

    public String getStorage()
    {
        return storage;
    }

    public void setStorage(String storage)
    {
        this.storage = storage;
    }

    public String getUserId()
    {
        return userId;
    }

    public void setUserId(String userId)
    {
        this.userId = userId;
    }
}

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
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;      // "шуруповёрт"
    private String location;  // "ящик №1"
    private String note;      // "с зарядкой"

    public Item() {}

    public Item(String name, String location, String note) {
        this.name = name;
        this.location = location;
        this.note = note;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getNote() { return note; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setLocation(String location) { this.location = location; }
    public void setNote(String note) { this.note = note; }
}

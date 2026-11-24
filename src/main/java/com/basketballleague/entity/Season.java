package com.basketballleague.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Season entity representing a competition period.
 * State changes are expressed through intention-revealing methods (ADR-0001: no public setters).
 */
@Entity
@Table(name = "seasons")
public class Season {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    /** Required by JPA. */
    protected Season() {
    }

    public Season(String name) {
        this.name = requireNonBlank(name, "Season name must not be blank");
    }

    public void rename(String newName) {
        this.name = requireNonBlank(newName, "Season name must not be blank");
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }
}

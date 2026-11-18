package com.basketballleague.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Basketball team entity.
 * State changes are expressed through intention-revealing methods (no public setters).
 */
@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @OneToMany(mappedBy = "team", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private final List<Player> players = new ArrayList<>();

    /** Required by JPA. */
    protected Team() {
    }

    public Team(String name, String city) {
        this.name = requireNonBlank(name, "Team name must not be blank");
        this.city = requireNonBlank(city, "City must not be blank");
    }

    // --- Intention-revealing methods (ADR-0001: no public setters) ---

    public void rename(String newName) {
        this.name = requireNonBlank(newName, "Team name must not be blank");
    }

    public void relocateTo(String newCity) {
        this.city = requireNonBlank(newCity, "City must not be blank");
    }

    List<Player> internalPlayers() {
        return players;
    }

    // --- Queries / Getters ---

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    private static String requireNonBlank(String value, String message) {
        Objects.requireNonNull(value, message);
        if (value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Team other = (Team) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Team{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", city='" + city + '\'' +
                '}';
    }
}

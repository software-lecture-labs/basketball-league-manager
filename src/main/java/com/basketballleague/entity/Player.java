package com.basketballleague.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Basketball player entity. A player always belongs to a team and holds only name, jersey number and position
 * (ADR-0001 data scope). State changes are expressed through intention-revealing methods (no public setters).
 */
@Entity
@Table(name = "players")
public class Player {

    static final int MIN_JERSEY_NUMBER = 0;
    static final int MAX_JERSEY_NUMBER = 99;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "jersey_number", nullable = false)
    private int jerseyNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "position", nullable = false, length = 2)
    private Position position;

    /** Required by JPA. */
    protected Player() {
    }

    public Player(Team team, String firstName, String lastName, int jerseyNumber, Position position) {
        this.firstName = requireNonBlank(firstName, "First name must not be blank");
        this.lastName = requireNonBlank(lastName, "Last name must not be blank");
        this.position = requirePosition(position);
        assignToTeam(requireTeam(team), jerseyNumber);
    }

    // --- Intention-revealing methods (ADR-0001: no public setters) ---

    /**
     * Moves this player to another team with the jersey number they will wear there.
     */
    public void transferTo(Team newTeam, int newJerseyNumber) {
        assignToTeam(requireTeam(newTeam), newJerseyNumber);
    }

    public void assignJerseyNumber(int number) {
        this.jerseyNumber = requireValidJerseyNumber(number);
    }

    public void changePosition(Position newPosition) {
        this.position = requirePosition(newPosition);
    }

    public void rename(String newFirstName, String newLastName) {
        this.firstName = requireNonBlank(newFirstName, "First name must not be blank");
        this.lastName = requireNonBlank(newLastName, "Last name must not be blank");
    }

    private void assignToTeam(Team newTeam, int number) {
        int validNumber = requireValidJerseyNumber(number);
        if (this.team != null && !this.team.equals(newTeam)) {
            this.team.internalPlayers().remove(this);
        }
        this.team = newTeam;
        this.jerseyNumber = validNumber;
        if (!newTeam.internalPlayers().contains(this)) {
            newTeam.internalPlayers().add(this);
        }
    }

    private static Team requireTeam(Team team) {
        if (team == null) {
            throw invalid("Team must not be null");
        }
        return team;
    }

    private static Position requirePosition(Position position) {
        if (position == null) {
            throw invalid("Position must not be null");
        }
        return position;
    }

    private static int requireValidJerseyNumber(int number) {
        if (number < MIN_JERSEY_NUMBER || number > MAX_JERSEY_NUMBER) {
            throw invalid("Jersey number must be between 0 and 99, was " + number);
        }
        return number;
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw invalid(message);
        }
        return value.strip();
    }

    private static RuntimeException invalid(String message) {
        return new IllegalArgumentException(message);
    }

    // --- Queries / Getters ---

    public Long getId() {
        return id;
    }

    public Team getTeam() {
        return team;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public int getJerseyNumber() {
        return jerseyNumber;
    }

    public Position getPosition() {
        return position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Player other = (Player) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Player{id=" + id + ", name='" + getFullName() + "', jerseyNumber=" + jerseyNumber
                + ", position=" + position + ", team=" + team.getName() + '}';
    }
}

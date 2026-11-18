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
import java.time.LocalDate;
import java.util.Objects;

/**
 * Basketball player entity.
 * State changes are expressed through intention-revealing methods (no public setters).
 */
@Entity
@Table(name = "players")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "jersey_number")
    private Integer jerseyNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "position", length = 2)
    private Position position;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    /** Required by JPA. */
    protected Player() {
    }

    /**
     * Creates a free-agent player without a team.
     */
    public Player(String firstName, String lastName, Position position, LocalDate birthDate) {
        this(null, firstName, lastName, null, position, birthDate);
    }

    /**
     * Creates a player assigned to a team with a jersey number.
     */
    public Player(Team team, String firstName, String lastName, Integer jerseyNumber, Position position, LocalDate birthDate) {
        this.firstName = requireNonBlank(firstName, "First name must not be blank");
        this.lastName = requireNonBlank(lastName, "Last name must not be blank");
        this.position = position;
        this.birthDate = birthDate;
        if (team != null) {
            assignToTeam(team, jerseyNumber);
        } else {
            validateJerseyNumber(jerseyNumber);
            this.jerseyNumber = jerseyNumber;
        }
    }

    // --- Intention-revealing methods (ADR-0001: no public setters) ---

    /**
     * Transfers this player to another team with an assigned jersey number.
     * Corresponds to the intention-revealing transferTo domain action (ADR-0001).
     */
    public void transferTo(Team newTeam, Integer newJerseyNumber) {
        Objects.requireNonNull(newTeam, "New team must not be null");
        assignToTeam(newTeam, newJerseyNumber);
    }

    /**
     * Releases this player from their current team, turning them into a free agent.
     */
    public void releaseFromTeam() {
        if (this.team != null) {
            this.team.internalPlayers().remove(this);
            this.team = null;
        }
        this.jerseyNumber = null;
    }

    /**
     * Assigns or changes the jersey number for this player.
     */
    public void assignJerseyNumber(Integer number) {
        validateJerseyNumber(number);
        this.jerseyNumber = number;
    }

    /**
     * Updates the player's on-court playing position.
     */
    public void changePosition(Position newPosition) {
        this.position = newPosition;
    }

    /**
     * Updates personal identification details.
     */
    public void updatePersonalInfo(String firstName, String lastName, LocalDate birthDate) {
        this.firstName = requireNonBlank(firstName, "First name must not be blank");
        this.lastName = requireNonBlank(lastName, "Last name must not be blank");
        this.birthDate = birthDate;
    }

    private void assignToTeam(Team newTeam, Integer number) {
        validateJerseyNumber(number);
        if (this.team != null && !this.team.equals(newTeam)) {
            this.team.internalPlayers().remove(this);
        }
        this.team = newTeam;
        this.jerseyNumber = number;
        if (!newTeam.internalPlayers().contains(this)) {
            newTeam.internalPlayers().add(this);
        }
    }

    private static void validateJerseyNumber(Integer number) {
        if (number != null && (number < 0 || number > 99)) {
            throw new IllegalArgumentException("Jersey number must be between 0 and 99");
        }
    }

    private static String requireNonBlank(String value, String message) {
        Objects.requireNonNull(value, message);
        if (value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
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

    public Integer getJerseyNumber() {
        return jerseyNumber;
    }

    public Position getPosition() {
        return position;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public boolean isFreeAgent() {
        return team == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player other = (Player) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Player{" +
                "id=" + id +
                ", name='" + getFullName() + '\'' +
                ", jerseyNumber=" + jerseyNumber +
                ", position=" + position +
                ", team=" + (team != null ? team.getName() : "Free Agent") +
                '}';
    }
}

package com.basketballleague.entity;

import com.basketballleague.domain.InvalidFixtureInputException;
import com.basketballleague.domain.InvalidSeasonException;
import com.basketballleague.domain.SeasonAlreadyStartedException;
import com.basketballleague.domain.SeasonFullException;
import com.basketballleague.domain.SeasonStatus;
import com.basketballleague.domain.TeamAlreadyInSeasonException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A league season and the teams taking part in it. Teams are referenced by id, not loaded as objects; games are
 * found by query, not held in a collection (ADR-0003).
 */
@Entity
@Table(name = "seasons")
public class Season {

    /** A double round-robin needs at least two teams. */
    static final int MIN_TEAMS = 2;
    /** 20 teams give 38 rounds and 380 games; the fixture is written in one transaction and returned unpaged. */
    static final int MAX_TEAMS = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private SeasonStatus status;

    @ElementCollection
    @CollectionTable(name = "season_teams", joinColumns = @JoinColumn(name = "season_id"))
    @Column(name = "team_id", nullable = false)
    private Set<Long> teamIds = new HashSet<>();

    /** Required by JPA. */
    protected Season() {
    }

    public Season(String name, LocalDate startDate) {
        this.name = requireNonBlank(name);
        this.startDate = requireStartDate(startDate);
        this.status = SeasonStatus.DRAFT;
    }

    // --- Intention-revealing methods (ADR-0003: no public setters) ---

    public void addTeam(long teamId) {
        if (teamId <= 0) {
            throw new InvalidSeasonException("Team id must be positive, was " + teamId);
        }
        if (!status.acceptsTeams()) {
            throw new SeasonAlreadyStartedException(id, status);
        }
        if (teamIds.contains(teamId)) {
            throw new TeamAlreadyInSeasonException(id, teamId);
        }
        if (teamIds.size() >= MAX_TEAMS) {
            throw new SeasonFullException(id, MAX_TEAMS);
        }
        teamIds.add(teamId);
    }

    /**
     * Starts the season; called when its fixture is generated. After this the team list is fixed, so a second
     * fixture for the same season is rejected here.
     */
    public void activate() {
        if (!status.canTransitionTo(SeasonStatus.ACTIVE)) {
            throw new SeasonAlreadyStartedException(id, status);
        }
        if (teamIds.size() < MIN_TEAMS) {
            throw new InvalidFixtureInputException("Season " + id + " has " + teamIds.size() + " teams");
        }
        this.status = SeasonStatus.ACTIVE;
    }

    private static String requireNonBlank(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidSeasonException("Season name must not be blank");
        }
        return value.strip();
    }

    private static LocalDate requireStartDate(LocalDate value) {
        if (value == null) {
            throw new InvalidSeasonException("Season start date must not be null");
        }
        return value;
    }

    // --- Queries / Getters ---

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public SeasonStatus getStatus() {
        return status;
    }

    /**
     * Team ids in ascending order. The set itself has no order; a fixed order keeps the generated fixture the
     * same for the same teams.
     */
    public List<Long> getTeamIds() {
        return teamIds.stream().sorted().toList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Season other = (Season) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Season{id=" + id + ", name='" + name + "', status=" + status + '}';
    }
}

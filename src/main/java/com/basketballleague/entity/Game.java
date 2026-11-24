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
import jakarta.persistence.Version;
import java.util.Objects;

/**
 * Game entity representing a match between two teams in a season.
 * State changes are expressed through intention-revealing methods (ADR-0001: no public setters).
 */
@Entity
@Table(name = "games")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "home_team_id", nullable = false)
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "away_team_id", nullable = false)
    private Team awayTeam;

    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "away_score")
    private Integer awayScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GameStatus status = GameStatus.SCHEDULED;

    @Version
    @Column(name = "version")
    private Long version;

    /** Required by JPA. */
    protected Game() {
    }

    public Game(Season season, Team homeTeam, Team awayTeam, int roundNumber) {
        this.season = Objects.requireNonNull(season, "season must not be null");
        this.homeTeam = Objects.requireNonNull(homeTeam, "homeTeam must not be null");
        this.awayTeam = Objects.requireNonNull(awayTeam, "awayTeam must not be null");
        if (homeTeam.equals(awayTeam)) {
            throw new IllegalArgumentException("Home and away team cannot be the same");
        }
        if (roundNumber <= 0) {
            throw new IllegalArgumentException("Round number must be positive");
        }
        this.roundNumber = roundNumber;
        this.status = GameStatus.SCHEDULED;
    }

    public void recordScores(int homeScore, int awayScore) {
        if (homeScore < 0 || awayScore < 0) {
            throw new IllegalArgumentException("Scores cannot be negative");
        }
        if (homeScore == awayScore) {
            throw new IllegalArgumentException("Basketball games cannot end in a draw");
        }
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.status = GameStatus.FINISHED;
    }

    public void confirm() {
        if (homeScore == null || awayScore == null) {
            throw new IllegalStateException("Scores must be recorded before confirming");
        }
        this.status = GameStatus.CONFIRMED;
    }

    public Long getId() {
        return id;
    }

    public Season getSeason() {
        return season;
    }

    public Team getHomeTeam() {
        return homeTeam;
    }

    public Team getAwayTeam() {
        return awayTeam;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public Integer getHomeScore() {
        return homeScore;
    }

    public Integer getAwayScore() {
        return awayScore;
    }

    public GameStatus getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }
}

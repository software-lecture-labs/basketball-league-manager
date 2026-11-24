package com.basketballleague.entity;

import com.basketballleague.domain.GameStatus;
import com.basketballleague.domain.InvalidGameException;
import com.basketballleague.domain.InvalidGameStatusTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * One game of a season's fixture. The season and the teams are referenced by id (ADR-0003). Scores and the
 * remaining transitions ({@code finish}, {@code confirm}) arrive with result entry.
 */
@Entity
@Table(name = "games")
public class Game {

    static final int FIRST_ROUND = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "season_id", nullable = false)
    private long seasonId;

    @Column(name = "round_no", nullable = false)
    private int roundNumber;

    @Column(name = "home_team_id", nullable = false)
    private long homeTeamId;

    @Column(name = "away_team_id", nullable = false)
    private long awayTeamId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private GameStatus status;

    /** Detects concurrent changes to the same game, e.g. two live score entries (ADR-0005). */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /** Required by JPA. */
    protected Game() {
    }

    public Game(long seasonId, int roundNumber, long homeTeamId, long awayTeamId) {
        this.seasonId = requirePositive(seasonId, "Season id");
        this.roundNumber = requireValidRound(roundNumber);
        this.homeTeamId = requirePositive(homeTeamId, "Home team id");
        this.awayTeamId = requirePositive(awayTeamId, "Away team id");
        if (homeTeamId == awayTeamId) {
            throw new InvalidGameException("A team cannot play against itself: " + homeTeamId);
        }
        this.status = GameStatus.SCHEDULED;
    }

    // --- Intention-revealing methods (ADR-0003: no public setters) ---

    /** The game tips off; live score entry is allowed from now on. */
    public void start() {
        transitionTo(GameStatus.LIVE);
    }

    private void transitionTo(GameStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidGameStatusTransitionException(id, status, target);
        }
        this.status = target;
    }

    private static long requirePositive(long value, String name) {
        if (value <= 0) {
            throw new InvalidGameException(name + " must be positive, was " + value);
        }
        return value;
    }

    private static int requireValidRound(int roundNumber) {
        if (roundNumber < FIRST_ROUND) {
            throw new InvalidGameException("Round number must be at least 1, was " + roundNumber);
        }
        return roundNumber;
    }

    // --- Queries / Getters ---

    public Long getId() {
        return id;
    }

    public long getSeasonId() {
        return seasonId;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public long getHomeTeamId() {
        return homeTeamId;
    }

    public long getAwayTeamId() {
        return awayTeamId;
    }

    public GameStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Game other = (Game) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Game{id=" + id + ", seasonId=" + seasonId + ", round=" + roundNumber + ", home=" + homeTeamId
                + ", away=" + awayTeamId + ", status=" + status + '}';
    }
}

package com.basketballleague.entity;

import com.basketballleague.domain.ConcurrentUpdateException;
import com.basketballleague.domain.GameStatus;
import com.basketballleague.domain.InvalidGameException;
import com.basketballleague.domain.InvalidGameStatusTransitionException;
import com.basketballleague.domain.InvalidPlayerPointsException;
import com.basketballleague.domain.InvalidQuarterScoreException;
import com.basketballleague.domain.OvertimeRequiredException;
import com.basketballleague.domain.PlayerNotInGameException;
import com.basketballleague.domain.ScoreMismatchException;
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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * One game of a season's fixture. The season and teams are referenced by id (ADR-0003); quarter scores and
 * player stats are owned value collections of this aggregate (ADR-0004).
 */
@Entity
@Table(name = "games")
public class Game {

    static final int FIRST_ROUND = 1;
    static final int REGULATION_QUARTERS = 4;

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

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "away_score")
    private Integer awayScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private GameStatus status;

    /** Detects concurrent changes to the same game, e.g. live score or confirmation conflicts (ADR-0004). */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @ElementCollection
    @CollectionTable(name = "game_quarter_scores", joinColumns = @JoinColumn(name = "game_id"))
    @OrderBy("quarterNo ASC")
    private List<QuarterScore> quarterScores = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "player_game_stats", joinColumns = @JoinColumn(name = "game_id"))
    @OrderBy("playerId ASC")
    private List<PlayerPoints> playerPoints = new ArrayList<>();

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

    // --- Intention-revealing methods (ADR-0003, ADR-0004: no public setters) ---

    /** The game tips off from SCHEDULED; live score entry is allowed from now on. */
    public void start() {
        if (this.status != GameStatus.SCHEDULED) {
            throw new InvalidGameStatusTransitionException(id, status, GameStatus.LIVE);
        }
        this.status = GameStatus.LIVE;
    }

    /**
     * Records or updates a single quarter sequentially while the game is LIVE. An overtime quarter (5+) is only
     * allowed when the cumulative score after the preceding period (4th quarter or prior overtime) is tied.
     */
    public void recordQuarter(QuarterScore quarter) {
        requireLive("record a quarter");
        if (quarter == null) {
            throw new InvalidQuarterScoreException("Quarter score must not be null");
        }
        final List<QuarterScore> updated = new ArrayList<>(this.quarterScores);
        final int index = quarter.getQuarterNo() - 1;
        if (index < updated.size()) {
            updated.set(index, quarter);
        } else if (index == updated.size()) {
            updated.add(quarter);
        } else {
            throw new InvalidQuarterScoreException(
                    "Expected quarter " + (updated.size() + 1) + ", got " + quarter.getQuarterNo());
        }
        validateQuarters(updated);
        replaceQuarterScores(updated);
    }

    /**
     * Replaces all quarter scores wholesale while the game is LIVE (PUT semantics, ADR-0004).
     */
    public void recordQuarters(List<QuarterScore> quarters) {
        requireLive("record quarters");
        if (quarters == null) {
            throw new InvalidQuarterScoreException("Quarter scores must not be null");
        }
        validateQuarters(quarters);
        replaceQuarterScores(quarters);
    }

    /**
     * Replaces all player points wholesale while the game is LIVE or FINISHED (PUT semantics, ADR-0004).
     */
    public void recordPlayerPoints(List<PlayerPoints> stats) {
        if (status != GameStatus.LIVE && status != GameStatus.FINISHED) {
            throw new InvalidGameStatusTransitionException(id, status, "record player points");
        }
        validatePlayerPoints(stats);
        this.playerPoints.clear();
        this.playerPoints.addAll(stats);
    }

    /**
     * Completes a LIVE game if at least four quarters are recorded and the score is not tied; stores the
     * computed team totals in {@code homeScore} and {@code awayScore} using {@link Math#addExact}.
     */
    public void finish() {
        if (!status.canTransitionTo(GameStatus.FINISHED)) {
            throw new InvalidGameStatusTransitionException(id, status, GameStatus.FINISHED);
        }
        if (quarterScores.size() < REGULATION_QUARTERS) {
            throw new InvalidQuarterScoreException(
                    "Game " + id + " requires at least " + REGULATION_QUARTERS
                            + " quarters to finish, had " + quarterScores.size());
        }
        final ScoreTotals totals = sumQuarters(quarterScores);
        if (totals.home() == totals.away()) {
            throw new OvertimeRequiredException(id, totals.home());
        }
        this.homeScore = totals.home();
        this.awayScore = totals.away();
        this.status = GameStatus.FINISHED;
    }

    /**
     * Reopens a FINISHED game back to LIVE so erroneous scores can be corrected before confirmation; clears the
     * stored final scores (ADR-0004).
     */
    public void reopen() {
        if (status != GameStatus.FINISHED) {
            throw new InvalidGameStatusTransitionException(id, status, GameStatus.LIVE);
        }
        this.homeScore = null;
        this.awayScore = null;
        this.status = GameStatus.LIVE;
    }

    /**
     * Confirms a FINISHED game if the caller holds the current version and each team's player points total
     * matches the team's game score (ADR-0004). No further changes are accepted afterwards.
     */
    public void confirm(long expectedVersion) {
        if (!status.canTransitionTo(GameStatus.CONFIRMED)) {
            throw new InvalidGameStatusTransitionException(id, status, GameStatus.CONFIRMED);
        }
        if (this.version != expectedVersion) {
            throw new ConcurrentUpdateException(id, expectedVersion, this.version);
        }
        verifyScoresBeforeConfirm();
        this.status = GameStatus.CONFIRMED;
    }

    private void requireLive(String action) {
        if (status != GameStatus.LIVE) {
            throw new InvalidGameStatusTransitionException(id, status, action);
        }
    }

    private static void validateQuarters(List<QuarterScore> quarters) {
        for (int index = 0; index < quarters.size(); index++) {
            final QuarterScore quarter = quarters.get(index);
            if (quarter == null) {
                throw new InvalidQuarterScoreException("Quarter score must not be null");
            }
            final int expectedQuarter = index + 1;
            if (quarter.getQuarterNo() != expectedQuarter) {
                throw new InvalidQuarterScoreException(
                        "Expected quarter " + expectedQuarter + ", got " + quarter.getQuarterNo());
            }
        }
        if (quarters.size() > REGULATION_QUARTERS) {
            validateOvertimeProgression(quarters);
        }
    }

    private static void validateOvertimeProgression(List<QuarterScore> quarters) {
        int runningHome = 0;
        int runningAway = 0;
        for (int index = 0; index < quarters.size() - 1; index++) {
            final QuarterScore quarter = quarters.get(index);
            runningHome = Math.addExact(runningHome, quarter.getHomePoints());
            runningAway = Math.addExact(runningAway, quarter.getAwayPoints());
            if (quarter.getQuarterNo() >= REGULATION_QUARTERS && runningHome != runningAway) {
                throw new InvalidQuarterScoreException(
                        "Overtime quarter " + (quarter.getQuarterNo() + 1)
                                + " is not allowed when score is not tied (" + runningHome + "-" + runningAway + ")");
            }
        }
    }

    private void replaceQuarterScores(List<QuarterScore> quarters) {
        this.quarterScores.clear();
        this.quarterScores.addAll(quarters);
    }

    private void validatePlayerPoints(List<PlayerPoints> stats) {
        if (stats == null) {
            throw new InvalidPlayerPointsException("Player points list must not be null");
        }
        final Set<Long> seenPlayers = new HashSet<>();
        for (PlayerPoints entry : stats) {
            if (entry == null) {
                throw new InvalidPlayerPointsException("Player points entry must not be null");
            }
            if (!isParticipantTeam(entry.getTeamId())) {
                throw new PlayerNotInGameException(entry.getPlayerId(), id, entry.getTeamId());
            }
            if (!seenPlayers.add(entry.getPlayerId())) {
                throw new InvalidPlayerPointsException("Duplicate stats for player " + entry.getPlayerId());
            }
        }
    }

    private boolean isParticipantTeam(long teamId) {
        return teamId == homeTeamId || teamId == awayTeamId;
    }

    private void verifyScoresBeforeConfirm() {
        if (homeScore == null || awayScore == null || homeScore.equals(awayScore)) {
            throw new ScoreMismatchException("Game " + id + " does not have valid finished scores");
        }
        if (quarterScores.size() < REGULATION_QUARTERS) {
            throw new InvalidQuarterScoreException(
                    "Game " + id + " has fewer than " + REGULATION_QUARTERS + " quarters");
        }
        final ScoreTotals quarterTotals = sumQuarters(quarterScores);
        if (quarterTotals.home() != homeScore || quarterTotals.away() != awayScore) {
            throw new ScoreMismatchException(
                    "Game " + id + " quarter totals (" + quarterTotals.home() + "-" + quarterTotals.away()
                            + ") do not match stored scores (" + homeScore + "-" + awayScore + ")");
        }
        final ScoreTotals playerTotals = sumPlayerPoints(playerPoints);
        if (playerTotals.home() != homeScore || playerTotals.away() != awayScore) {
            throw new ScoreMismatchException(id, homeScore, playerTotals.home(), awayScore, playerTotals.away());
        }
    }

    private static ScoreTotals sumQuarters(List<QuarterScore> quarters) {
        int homeTotal = 0;
        int awayTotal = 0;
        for (QuarterScore quarter : quarters) {
            homeTotal = Math.addExact(homeTotal, quarter.getHomePoints());
            awayTotal = Math.addExact(awayTotal, quarter.getAwayPoints());
        }
        return new ScoreTotals(homeTotal, awayTotal);
    }

    private ScoreTotals sumPlayerPoints(List<PlayerPoints> stats) {
        int homeTotal = 0;
        int awayTotal = 0;
        for (PlayerPoints entry : stats) {
            if (entry.getTeamId() == homeTeamId) {
                homeTotal = Math.addExact(homeTotal, entry.getPoints());
            } else if (entry.getTeamId() == awayTeamId) {
                awayTotal = Math.addExact(awayTotal, entry.getPoints());
            } else {
                throw new PlayerNotInGameException(entry.getPlayerId(), id, entry.getTeamId());
            }
        }
        return new ScoreTotals(homeTotal, awayTotal);
    }

    private record ScoreTotals(int home, int away) {
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

    public Integer getHomeScore() {
        return homeScore;
    }

    public Integer getAwayScore() {
        return awayScore;
    }

    public GameStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }

    public List<QuarterScore> getQuarterScores() {
        return Collections.unmodifiableList(quarterScores);
    }

    public List<PlayerPoints> getPlayerPoints() {
        return Collections.unmodifiableList(playerPoints);
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

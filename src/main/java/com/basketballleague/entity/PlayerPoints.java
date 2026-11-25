package com.basketballleague.entity;

import com.basketballleague.domain.InvalidGameException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Value object representing a player's points in a {@link Game} (ADR-0004). Stores the team id the player
 * belonged to when stats were recorded so later transfers do not alter historical game statistics.
 */
@Embeddable
public class PlayerPoints {

    @Column(name = "player_id", nullable = false)
    private long playerId;

    @Column(name = "team_id", nullable = false)
    private long teamId;

    @Column(name = "points", nullable = false)
    private int points;

    /** Required by JPA. */
    protected PlayerPoints() {
    }

    public PlayerPoints(long playerId, long teamId, int points) {
        this.playerId = requirePositive(playerId, "Player id");
        this.teamId = requirePositive(teamId, "Team id");
        this.points = requireNonNegative(points);
    }

    private static long requirePositive(long value, String label) {
        if (value <= 0) {
            throw new InvalidGameException(label + " must be positive, was " + value);
        }
        return value;
    }

    private static int requireNonNegative(int points) {
        if (points < 0) {
            throw new InvalidGameException("Player points must not be negative, was " + points);
        }
        return points;
    }

    public long getPlayerId() {
        return playerId;
    }

    public long getTeamId() {
        return teamId;
    }

    public int getPoints() {
        return points;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PlayerPoints other = (PlayerPoints) o;
        return playerId == other.playerId && teamId == other.teamId && points == other.points;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, teamId, points);
    }

    @Override
    public String toString() {
        return "PlayerPoints{playerId=" + playerId + ", teamId=" + teamId + ", points=" + points + '}';
    }
}

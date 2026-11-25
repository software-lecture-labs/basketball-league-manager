package com.basketballleague.domain;

import java.util.Objects;

/**
 * Represents the final score of a confirmed match between two teams.
 * In basketball, matches cannot end in a draw.
 */
public record MatchResult(
        Long homeTeamId,
        Long awayTeamId,
        int homeScore,
        int awayScore
) {
    public MatchResult {
        Objects.requireNonNull(homeTeamId, "homeTeamId");
        Objects.requireNonNull(awayTeamId, "awayTeamId");
        if (homeTeamId.equals(awayTeamId)) {
            throw new IllegalArgumentException("Home and away team cannot be the same: " + homeTeamId);
        }
        if (homeScore < 0 || awayScore < 0) {
            throw new IllegalArgumentException("Scores cannot be negative: " + homeScore + " - " + awayScore);
        }
        if (homeScore == awayScore) {
            throw new IllegalArgumentException(
                    "Basketball matches cannot end in a draw: " + homeScore + " - " + awayScore);
        }
    }

    public boolean isBetween(Long teamA, Long teamB) {
        Objects.requireNonNull(teamA, "teamA");
        Objects.requireNonNull(teamB, "teamB");
        return (homeTeamId.equals(teamA) && awayTeamId.equals(teamB))
                || (homeTeamId.equals(teamB) && awayTeamId.equals(teamA));
    }

    public Long winnerId() {
        return homeScore > awayScore ? homeTeamId : awayTeamId;
    }

    public Long loserId() {
        return homeScore > awayScore ? awayTeamId : homeTeamId;
    }

    public int scoreFor(Long teamId) {
        Objects.requireNonNull(teamId, "teamId");
        if (homeTeamId.equals(teamId)) return homeScore;
        if (awayTeamId.equals(teamId)) return awayScore;
        throw new IllegalArgumentException("Team " + teamId + " did not participate in match " + this);
    }

    public int scoreAgainst(Long teamId) {
        Objects.requireNonNull(teamId, "teamId");
        if (homeTeamId.equals(teamId)) return awayScore;
        if (awayTeamId.equals(teamId)) return homeScore;
        throw new IllegalArgumentException("Team " + teamId + " did not participate in match " + this);
    }
}

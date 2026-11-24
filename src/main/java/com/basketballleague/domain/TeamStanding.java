package com.basketballleague.domain;

import java.util.Objects;

/**
 * An immutable standing record representing a team's statistical summary in the league table.
 */
public record TeamStanding(
        Long teamId,
        String teamName,
        int played,
        int won,
        int lost,
        int pointsScored,
        int pointsConceded,
        int pointDifference,
        int points
) {
    public TeamStanding {
        Objects.requireNonNull(teamId, "teamId");
        Objects.requireNonNull(teamName, "teamName");
        if (teamName.isBlank()) {
            throw new IllegalArgumentException("Team name cannot be blank");
        }
        if (played < 0) throw new IllegalArgumentException("Played matches cannot be negative: " + played);
        if (won < 0) throw new IllegalArgumentException("Won matches cannot be negative: " + won);
        if (lost < 0) throw new IllegalArgumentException("Lost matches cannot be negative: " + lost);
        if (played != won + lost) {
            throw new IllegalArgumentException("Played (" + played + ") must equal won (" + won + ") + lost (" + lost + ")");
        }
        if (pointsScored < 0) throw new IllegalArgumentException("Points scored cannot be negative: " + pointsScored);
        if (pointsConceded < 0) throw new IllegalArgumentException("Points conceded cannot be negative: " + pointsConceded);
        if (pointDifference != pointsScored - pointsConceded) {
            throw new IllegalArgumentException("Point difference (" + pointDifference + ") must equal pointsScored ("
                    + pointsScored + ") - pointsConceded (" + pointsConceded + ")");
        }
    }

    public static TeamStanding of(
            Long teamId,
            String teamName,
            int won,
            int lost,
            int pointsScored,
            int pointsConceded,
            PointsPolicy pointsPolicy) {
        Objects.requireNonNull(pointsPolicy, "pointsPolicy");
        int played = won + lost;
        int diff = pointsScored - pointsConceded;
        int points = pointsPolicy.calculatePoints(won, lost);
        return new TeamStanding(teamId, teamName, played, won, lost, pointsScored, pointsConceded, diff, points);
    }

    public static TeamStanding zero(Long teamId, String teamName) {
        return new TeamStanding(teamId, teamName, 0, 0, 0, 0, 0, 0, 0);
    }
}

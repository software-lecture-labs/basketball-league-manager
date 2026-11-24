package com.basketballleague.domain;

/**
 * Defines the scoring policy for matches in the league.
 * Standard basketball rules award 2 points for a win and 1 point for a loss.
 */
public interface PointsPolicy {

    int pointsForWin();

    int pointsForLoss();

    default int calculatePoints(int wins, int losses) {
        if (wins < 0) {
            throw new IllegalArgumentException("Wins cannot be negative: " + wins);
        }
        if (losses < 0) {
            throw new IllegalArgumentException("Losses cannot be negative: " + losses);
        }
        return (wins * pointsForWin()) + (losses * pointsForLoss());
    }
}

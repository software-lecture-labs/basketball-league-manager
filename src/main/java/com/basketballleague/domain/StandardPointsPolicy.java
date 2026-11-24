package com.basketballleague.domain;

/**
 * Standard points policy where a win awards 2 points and a loss awards 1 point.
 * Matches cannot end in a draw in basketball.
 */
public final class StandardPointsPolicy implements PointsPolicy {

    private static final int DEFAULT_WIN_POINTS = 2;
    private static final int DEFAULT_LOSS_POINTS = 1;

    public static final StandardPointsPolicy INSTANCE = new StandardPointsPolicy();

    private final int winPoints;
    private final int lossPoints;

    public StandardPointsPolicy() {
        this(DEFAULT_WIN_POINTS, DEFAULT_LOSS_POINTS);
    }

    public StandardPointsPolicy(int winPoints, int lossPoints) {
        if (winPoints < 0) {
            throw new IllegalArgumentException("Points for win cannot be negative: " + winPoints);
        }
        if (lossPoints < 0) {
            throw new IllegalArgumentException("Points for loss cannot be negative: " + lossPoints);
        }
        this.winPoints = winPoints;
        this.lossPoints = lossPoints;
    }

    @Override
    public int pointsForWin() {
        return winPoints;
    }

    @Override
    public int pointsForLoss() {
        return lossPoints;
    }
}

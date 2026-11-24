package com.basketballleague.domain;

import java.util.Objects;

/**
 * Tie-breaker rule based on total points scored (atılan sayı).
 * The team with more points scored ranks ahead.
 */
public final class PointsScoredTieBreaker implements TieBreaker {

    @Override
    public int breakTie(TeamStanding a, TeamStanding b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        return Integer.compare(b.pointsScored(), a.pointsScored());
    }
}

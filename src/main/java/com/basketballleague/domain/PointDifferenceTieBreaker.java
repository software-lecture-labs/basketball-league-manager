package com.basketballleague.domain;

import java.util.Objects;

/**
 * Tie-breaker rule based on overall point difference (genel averaj: atılan sayı - yenilen sayı).
 * The team with the higher point difference ranks ahead.
 */
public final class PointDifferenceTieBreaker implements TieBreaker {

    @Override
    public int breakTie(TeamStanding a, TeamStanding b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        return Integer.compare(b.pointDifference(), a.pointDifference());
    }
}

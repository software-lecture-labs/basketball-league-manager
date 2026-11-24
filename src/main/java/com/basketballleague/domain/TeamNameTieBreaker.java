package com.basketballleague.domain;

import java.util.Objects;

/**
 * Final tie-breaker rule based on team name in alphabetical order (A -> Z).
 */
public final class TeamNameTieBreaker implements TieBreaker {

    @Override
    public int breakTie(TeamStanding a, TeamStanding b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        return a.teamName().compareToIgnoreCase(b.teamName());
    }
}

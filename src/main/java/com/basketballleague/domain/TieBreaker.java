package com.basketballleague.domain;

import java.util.Collection;
import java.util.Objects;

/**
 * Strategy interface for breaking ties between teams with equal points in the league table.
 *
 * <p>Contract:
 * <ul>
 *   <li>Returns a negative integer if {@code a} ranks ahead of {@code b}.</li>
 *   <li>Returns a positive integer if {@code b} ranks ahead of {@code a}.</li>
 *   <li>Returns zero if the tie cannot be broken by this rule.</li>
 * </ul>
 */
@FunctionalInterface
public interface TieBreaker {

    int breakTie(TeamStanding a, TeamStanding b);

    default TieBreaker then(TieBreaker next) {
        Objects.requireNonNull(next, "next");
        return (a, b) -> {
            int result = this.breakTie(a, b);
            return (result != 0) ? result : next.breakTie(a, b);
        };
    }

    /**
     * Constructs the standard tie-breaker chain specified by the league rules:
     * İkili averaj (H2H) -> Genel averaj (Point difference) -> Atılan sayı (Points scored) -> Takım adı (A -> Z).
     */
    static TieBreaker standardChain(Collection<MatchResult> confirmedMatches, PointsPolicy pointsPolicy) {
        return new HeadToHeadTieBreaker(confirmedMatches, pointsPolicy)
                .then(new PointDifferenceTieBreaker())
                .then(new PointsScoredTieBreaker())
                .then(new TeamNameTieBreaker());
    }

    static TieBreaker standardChain(Collection<MatchResult> confirmedMatches) {
        return standardChain(confirmedMatches, StandardPointsPolicy.INSTANCE);
    }
}

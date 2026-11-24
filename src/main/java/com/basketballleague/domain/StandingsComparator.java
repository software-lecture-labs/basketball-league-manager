package com.basketballleague.domain;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;

/**
 * Primary comparator used to sort the complete league standings table.
 * First orders by league points descending, then delegates ties to the configured {@link TieBreaker}.
 */
public final class StandingsComparator implements Comparator<TeamStanding> {

    private final TieBreaker tieBreaker;

    public StandingsComparator(TieBreaker tieBreaker) {
        this.tieBreaker = Objects.requireNonNull(tieBreaker, "tieBreaker");
    }

    public static StandingsComparator ofStandard(Collection<MatchResult> confirmedMatches, PointsPolicy pointsPolicy) {
        return new StandingsComparator(TieBreaker.standardChain(confirmedMatches, pointsPolicy));
    }

    public static StandingsComparator ofStandard(Collection<MatchResult> confirmedMatches) {
        return new StandingsComparator(TieBreaker.standardChain(confirmedMatches));
    }

    @Override
    public int compare(TeamStanding a, TeamStanding b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        int pointsComparison = Integer.compare(b.points(), a.points());
        if (pointsComparison != 0) {
            return pointsComparison;
        }
        return tieBreaker.breakTie(a, b);
    }
}

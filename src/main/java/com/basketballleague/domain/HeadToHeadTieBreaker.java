package com.basketballleague.domain;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Tie-breaker rule based on head-to-head (ikili averaj) results between tied teams.
 * Only confirmed matches between the two tied teams are evaluated:
 * <ol>
 *   <li>Team with more points in their head-to-head matches ranks ahead.</li>
 *   <li>If head-to-head points are equal, team with better head-to-head point difference ranks ahead.</li>
 *   <li>If still equal or if no matches were played between them, returns 0.</li>
 * </ol>
 */
public final class HeadToHeadTieBreaker implements TieBreaker {

    private final List<MatchResult> matches;
    private final PointsPolicy pointsPolicy;

    public HeadToHeadTieBreaker(Collection<MatchResult> confirmedMatches, PointsPolicy pointsPolicy) {
        this.matches = confirmedMatches == null ? List.of() : List.copyOf(confirmedMatches);
        this.pointsPolicy = Objects.requireNonNull(pointsPolicy, "pointsPolicy");
    }

    public HeadToHeadTieBreaker(Collection<MatchResult> confirmedMatches) {
        this(confirmedMatches, StandardPointsPolicy.INSTANCE);
    }

    @Override
    public int breakTie(TeamStanding a, TeamStanding b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        if (a.teamId().equals(b.teamId())) {
            return 0;
        }

        final List<MatchResult> h2h = matches.stream()
                .filter(m -> m.isBetween(a.teamId(), b.teamId()))
                .toList();

        if (h2h.isEmpty()) {
            return 0;
        }

        int h2hWinsA = 0;
        int h2hLossesA = 0;
        int h2hPointsScoredA = 0;
        int h2hPointsScoredB = 0;

        for (MatchResult match : h2h) {
            if (match.winnerId().equals(a.teamId())) {
                h2hWinsA++;
            } else {
                h2hLossesA++;
            }
            h2hPointsScoredA += match.scoreFor(a.teamId());
            h2hPointsScoredB += match.scoreFor(b.teamId());
        }

        int h2hWinsB = h2hLossesA;
        int h2hLossesB = h2hWinsA;

        int h2hPointsA = pointsPolicy.calculatePoints(h2hWinsA, h2hLossesA);
        int h2hPointsB = pointsPolicy.calculatePoints(h2hWinsB, h2hLossesB);

        int pointsComparison = Integer.compare(h2hPointsB, h2hPointsA);
        if (pointsComparison != 0) {
            return pointsComparison;
        }

        int diffA = h2hPointsScoredA - h2hPointsScoredB;
        int diffB = h2hPointsScoredB - h2hPointsScoredA;

        return Integer.compare(diffB, diffA);
    }
}

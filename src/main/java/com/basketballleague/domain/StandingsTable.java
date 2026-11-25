package com.basketballleague.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds the ranked league table from confirmed results. The table is computed on every read, never stored
 * (ADR-0001). Pure Java, so every ranking rule is unit tested without a database.
 */
public final class StandingsTable {

    private StandingsTable() {
    }

    /**
     * Ranks every team in {@code teamNames}; a team without results is listed with zeros. Results that involve a
     * team outside {@code teamNames} are ignored. Order: points, then the {@link TieBreaker#standardChain} rules.
     */
    public static List<TeamStanding> rank(Map<Long, String> teamNames, Collection<MatchResult> results,
            PointsPolicy pointsPolicy) {
        Objects.requireNonNull(teamNames, "teamNames");
        Objects.requireNonNull(results, "results");
        Objects.requireNonNull(pointsPolicy, "pointsPolicy");

        final Map<Long, Totals> totals = new LinkedHashMap<>();
        teamNames.keySet().forEach(teamId -> totals.put(teamId, new Totals()));
        final List<MatchResult> counted = new ArrayList<>();
        for (MatchResult result : results) {
            final Totals home = totals.get(result.homeTeamId());
            final Totals away = totals.get(result.awayTeamId());
            if (home != null && away != null) {
                home.add(result.homeScore(), result.awayScore());
                away.add(result.awayScore(), result.homeScore());
                counted.add(result);
            }
        }

        final List<TeamStanding> table = new ArrayList<>();
        totals.forEach((teamId, t) -> table.add(TeamStanding.of(teamId, teamNames.get(teamId), t.won, t.lost,
                t.scored, t.conceded, pointsPolicy)));
        table.sort(StandingsComparator.ofStandard(counted, pointsPolicy));
        return List.copyOf(table);
    }

    /** Running totals of one team; {@code addExact} fails loudly instead of wrapping around. */
    private static final class Totals {

        private int won;
        private int lost;
        private int scored;
        private int conceded;

        void add(int pointsFor, int pointsAgainst) {
            if (pointsFor > pointsAgainst) {
                won = Math.addExact(won, 1);
            } else {
                lost = Math.addExact(lost, 1);
            }
            scored = Math.addExact(scored, pointsFor);
            conceded = Math.addExact(conceded, pointsAgainst);
        }
    }
}

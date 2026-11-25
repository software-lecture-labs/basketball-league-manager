package com.basketballleague.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StandingsTableTest {

    private static final PointsPolicy POLICY = StandardPointsPolicy.INSTANCE;

    private static Map<Long, String> teams(String... names) {
        final Map<Long, String> teams = new LinkedHashMap<>();
        for (int index = 0; index < names.length; index++) {
            teams.put(index + 1L, names[index]);
        }
        return teams;
    }

    private static List<String> names(List<TeamStanding> table) {
        return table.stream().map(TeamStanding::teamName).toList();
    }

    @Test
    void teamsWithoutResultsAreListedWithZerosInTurkishAlphabeticalOrder() {
        final List<TeamStanding> table = StandingsTable.rank(
                teams("Olympiacos", "Ülker", "Anadolu Efes", "İstanbul BB", "Žalgiris"), List.of(), POLICY);

        assertThat(names(table)).containsExactly("Anadolu Efes", "İstanbul BB", "Olympiacos", "Ülker", "Žalgiris");
        assertThat(table).allSatisfy(row -> {
            assertThat(row.played()).isZero();
            assertThat(row.points()).isZero();
        });
    }

    @Test
    void countsWinsLossesPointsAndScoresFromEachResult() {
        final List<TeamStanding> table = StandingsTable.rank(teams("A", "B", "C"), List.of(
                new MatchResult(1L, 2L, 80, 70),
                new MatchResult(3L, 1L, 60, 90),
                new MatchResult(2L, 3L, 75, 70)), POLICY);

        assertThat(table).containsExactly(
                new TeamStanding(1L, "A", 2, 2, 0, 170, 130, 40, 4),
                new TeamStanding(2L, "B", 2, 1, 1, 145, 150, -5, 3),
                new TeamStanding(3L, "C", 2, 0, 2, 130, 165, -35, 2));
    }

    @Test
    void headToHeadDecidesBeforePointDifference() {
        // A and B both win once and lose once (3 points). A has the far better difference (+39 vs -19),
        // but B won the game between them, so B ranks first. D has 2 points, C has 1.
        final List<TeamStanding> table = StandingsTable.rank(teams("A", "B", "C", "D"), List.of(
                new MatchResult(2L, 1L, 71, 70),
                new MatchResult(1L, 3L, 100, 60),
                new MatchResult(2L, 4L, 60, 80)), POLICY);

        assertThat(names(table)).containsExactly("B", "A", "D", "C");
        assertThat(table.get(0).pointDifference()).isLessThan(table.get(1).pointDifference());
    }

    @Test
    void pointDifferenceDecidesWhenTiedTeamsHaveNotMet() {
        // A and B never met; each beat C and lost to D (3 points). A's difference is +25, B's is -9.
        final List<TeamStanding> table = StandingsTable.rank(teams("A", "B", "C", "D"), List.of(
                new MatchResult(1L, 3L, 90, 60),
                new MatchResult(4L, 1L, 75, 70),
                new MatchResult(2L, 3L, 81, 80),
                new MatchResult(4L, 2L, 70, 60)), POLICY);

        assertThat(names(table)).containsExactly("D", "A", "B", "C");
    }

    @Test
    void ignoresResultsOfTeamsOutsideTheSeason() {
        final List<TeamStanding> table = StandingsTable.rank(teams("A", "B"),
                List.of(new MatchResult(1L, 99L, 80, 70)), POLICY);

        assertThat(table).allSatisfy(row -> assertThat(row.played()).isZero());
    }

    @Test
    void scoreTotalsFailLoudlyInsteadOfOverflowing() {
        final List<MatchResult> results = List.of(
                new MatchResult(1L, 2L, Integer.MAX_VALUE, 0),
                new MatchResult(1L, 2L, 1, 0));

        assertThatThrownBy(() -> StandingsTable.rank(teams("A", "B"), results, POLICY))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void returnedTableCannotBeChanged() {
        final List<TeamStanding> table = StandingsTable.rank(teams("A"), List.of(), POLICY);

        assertThatThrownBy(table::clear).isInstanceOf(UnsupportedOperationException.class);
    }
}

package com.basketballleague.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TieBreakerTest {

    private final PointsPolicy pointsPolicy = StandardPointsPolicy.INSTANCE;

    // --- 1. İkili Averaj (Head-to-head) Testleri ---

    @Test
    @DisplayName("İkili averaj: İki maçı da kazanan takım öne geçer")
    void headToHeadRanksTeamWithMoreWinsAhead() {
        final Long efesId = 1L;
        final Long realId = 2L;

        // Efes beat Real Madrid in both home and away matches
        final List<MatchResult> matches = List.of(
                new MatchResult(efesId, realId, 85, 80),
                new MatchResult(realId, efesId, 78, 82)
        );

        final TieBreaker breaker = new HeadToHeadTieBreaker(matches, pointsPolicy);

        final TeamStanding efes = TeamStanding.of(efesId, "Anadolu Efes", 10, 5, 1200, 1100, pointsPolicy);
        final TeamStanding real = TeamStanding.of(realId, "Real Madrid", 10, 5, 1250, 1050, pointsPolicy);

        // Negative value means efes precedes real (ranked higher)
        assertThat(breaker.breakTie(efes, real)).isNegative();
        assertThat(breaker.breakTie(real, efes)).isPositive();
    }

    @ParameterizedTest(name = "Efes vs Real H2H scores: {0}-{1} and {2}-{3} -> expected comparison {4}")
    @CsvSource({
            "80, 70, 75, 70, -1",  // Efes +10 at home, Real +5 at home -> Efes +5 diff -> Efes ranks ahead (-1)
            "70, 80, 70, 75, 1",   // Real +10 at home, Efes +5 at home -> Real +5 diff -> Real ranks ahead (1)
            "80, 75, 85, 80, 0"    // Efes +5 at home, Real +5 at home -> Equal H2H diff -> Tie remains (0)
    })
    void headToHeadBreaksTieByPointDifferenceWhenWinsAreEqual(
            int match1HomeScore, int match1AwayScore,
            int match2HomeScore, int match2AwayScore,
            int expectedSign) {

        final Long efesId = 1L;
        final Long realId = 2L;

        // Match 1: Efes (home) vs Real (away)
        // Match 2: Real (home) vs Efes (away)
        final List<MatchResult> matches = List.of(
                new MatchResult(efesId, realId, match1HomeScore, match1AwayScore),
                new MatchResult(realId, efesId, match2HomeScore, match2AwayScore)
        );

        final TieBreaker breaker = new HeadToHeadTieBreaker(matches, pointsPolicy);

        final TeamStanding efes = TeamStanding.of(efesId, "Anadolu Efes", 10, 5, 1200, 1100, pointsPolicy);
        final TeamStanding real = TeamStanding.of(realId, "Real Madrid", 10, 5, 1250, 1050, pointsPolicy);

        final int result = breaker.breakTie(efes, real);
        if (expectedSign < 0) {
            assertThat(result).isNegative();
        } else if (expectedSign > 0) {
            assertThat(result).isPositive();
        } else {
            assertThat(result).isZero();
        }
    }

    @Test
    @DisplayName("İkili averaj: Takımlar arasında oynanmış maç yoksa eşitlik bozulamaz (0 döner)")
    void headToHeadReturnsZeroWhenNoMatchesPlayed() {
        final Long efesId = 1L;
        final Long realId = 2L;
        final Long fenerId = 3L;

        // Match only between Efes and Fener, none between Efes and Real
        final List<MatchResult> matches = List.of(
                new MatchResult(efesId, fenerId, 90, 85)
        );

        final TieBreaker breaker = new HeadToHeadTieBreaker(matches, pointsPolicy);

        final TeamStanding efes = TeamStanding.of(efesId, "Anadolu Efes", 10, 5, 1200, 1100, pointsPolicy);
        final TeamStanding real = TeamStanding.of(realId, "Real Madrid", 10, 5, 1250, 1050, pointsPolicy);

        assertThat(breaker.breakTie(efes, real)).isZero();
    }

    // --- 2. Genel Averaj (Point difference) Testleri ---

    @ParameterizedTest(name = "Team A diff={0}, Team B diff={1} -> expected sign {2}")
    @CsvSource({
            "50, 20, -1",   // A has better positive difference -> A ranks ahead (-1)
            "20, 50, 1",    // B has better positive difference -> B ranks ahead (1)
            "10, -5, -1",   // A positive, B negative -> A ranks ahead (-1)
            "-5, -20, -1",  // Both negative, A is less negative (-5 > -20) -> A ranks ahead (-1)
            "-20, -5, 1",   // B is less negative -> B ranks ahead (1)
            "15, 15, 0",    // Equal difference -> tie remains (0)
            "0, 0, 0"       // Both zero difference -> tie remains (0)
    })
    void pointDifferenceTieBreakerRanksHigherDifferenceAhead(int diffA, int diffB, int expectedSign) {
        final TieBreaker breaker = new PointDifferenceTieBreaker();

        final TeamStanding a = new TeamStanding(1L, "Team A", 10, 5, 5, 1000 + diffA, 1000, diffA, 15);
        final TeamStanding b = new TeamStanding(2L, "Team B", 10, 5, 5, 1000 + diffB, 1000, diffB, 15);

        final int result = breaker.breakTie(a, b);
        if (expectedSign < 0) {
            assertThat(result).isNegative();
        } else if (expectedSign > 0) {
            assertThat(result).isPositive();
        } else {
            assertThat(result).isZero();
        }
    }

    // --- 3. Atılan Sayı (Points scored) Testleri ---

    @ParameterizedTest(name = "Team A scored={0}, Team B scored={1} -> expected sign {2}")
    @CsvSource({
            "850, 800, -1",  // A scored more -> A ranks ahead (-1)
            "790, 840, 1",   // B scored more -> B ranks ahead (1)
            "900, 900, 0"    // Equal scored -> tie remains (0)
    })
    void pointsScoredTieBreakerRanksHigherScoredAhead(int scoredA, int scoredB, int expectedSign) {
        final TieBreaker breaker = new PointsScoredTieBreaker();

        final TeamStanding a = new TeamStanding(1L, "Team A", 10, 5, 5, scoredA, scoredA - 10, 10, 15);
        final TeamStanding b = new TeamStanding(2L, "Team B", 10, 5, 5, scoredB, scoredB - 10, 10, 15);

        final int result = breaker.breakTie(a, b);
        if (expectedSign < 0) {
            assertThat(result).isNegative();
        } else if (expectedSign > 0) {
            assertThat(result).isPositive();
        } else {
            assertThat(result).isZero();
        }
    }

    // --- 4. Takım Adı (A -> Z) Testleri ---

    @ParameterizedTest(name = "Name A=''{0}'', Name B=''{1}'' -> expected sign {2}")
    @CsvSource({
            "Anadolu Efes, Fenerbahçe Beko, -1",
            "Panathinaikos, Olympiacos, 1",
            "Real Madrid, Barcelona, 1",
            "EA7 Olimpia Milano, LDLC ASVEL, -1",
            "ASVEL, asvel, 0"
    })
    void teamNameTieBreakerRanksAlphabeticallyAscending(String nameA, String nameB, int expectedSign) {
        final TieBreaker breaker = new TeamNameTieBreaker();

        final TeamStanding a = TeamStanding.of(1L, nameA, 5, 5, 800, 800, pointsPolicy);
        final TeamStanding b = TeamStanding.of(2L, nameB, 5, 5, 800, 800, pointsPolicy);

        final int result = breaker.breakTie(a, b);
        if (expectedSign < 0) {
            assertThat(result).isNegative();
        } else if (expectedSign > 0) {
            assertThat(result).isPositive();
        } else {
            assertThat(result).isZero();
        }
    }

    // --- 4b. Türkçe Karakterli Takım Adı Testleri ---
    // compareToIgnoreCase Türkçe özel harfleri ('İ','Ö','Ş','Ü') yanlış sıralar;
    // Collator(tr-TR) + PRIMARY strength ile bu durumlar düzeltilir.

    @ParameterizedTest(name = "Türkçe: ''{0}'' vs ''{1}'' beklenen: {2}")
    @CsvSource({
            // 'İ' Türkçe alfabesinde 'I' ile farklı; compareToIgnoreCase bunu yanlış sıralıyordu
            "İstanbul Hawks, Juventus Roma, -1",
            // 'Ö' 'O'dan sonra, 'P'den önce
            "Özel Spor, Panathinaikos, -1",
            // 'Ş' 'S'den sonra, 'T'den önce
            "Şanlıurfa Spor, Trabzonspor, -1",
            // 'Ü' 'U'dan sonra
            "Üsküdar Basket, Valencia Basket, -1",
            // PRIMARY strength: 'İ' ve 'i' aynı temel karaktere sahip -> eşit
            "İzmir Basket, izmir basket, 0"
    })
    void teamNameTieBreakerHandlesTurkishCharacters(String nameA, String nameB, int expectedSign) {
        final TieBreaker breaker = new TeamNameTieBreaker();

        final TeamStanding a = TeamStanding.of(1L, nameA, 5, 5, 800, 800, pointsPolicy);
        final TeamStanding b = TeamStanding.of(2L, nameB, 5, 5, 800, 800, pointsPolicy);

        final int result = breaker.breakTie(a, b);
        if (expectedSign < 0) {
            assertThat(result).isNegative();
        } else if (expectedSign > 0) {
            assertThat(result).isPositive();
        } else {
            assertThat(result).isZero();
        }
    }

    // --- 5. Zincir (Chain: İkili Averaj -> Genel Averaj -> Atılan Sayı -> Takım Adı) Entegrasyon Testleri ---

    @Test
    @DisplayName("Zincir: İkili averaj eşitse genel averaj eşitliği bozar")
    void chainFallsBackToPointDifferenceWhenHeadToHeadIsTied() {
        final Long team1Id = 1L;
        final Long team2Id = 2L;

        // Exactly identical H2H matches (each won by 5 points)
        final List<MatchResult> matches = List.of(
                new MatchResult(team1Id, team2Id, 85, 80),
                new MatchResult(team2Id, team1Id, 85, 80)
        );

        final TieBreaker chain = TieBreaker.standardChain(matches, pointsPolicy);

        // Same points (15), tied H2H, but Team 1 has better overall point difference (+40 vs +20)
        final TeamStanding team1 = new TeamStanding(team1Id, "Team 1", 10, 5, 5, 840, 800, 40, 15);
        final TeamStanding team2 = new TeamStanding(team2Id, "Team 2", 10, 5, 5, 820, 800, 20, 15);

        assertThat(chain.breakTie(team1, team2)).isNegative();
        assertThat(chain.breakTie(team2, team1)).isPositive();
    }

    @Test
    @DisplayName("Zincir: İkili averaj ve genel averaj eşitse atılan sayı eşitliği bozar")
    void chainFallsBackToPointsScoredWhenPointDifferenceIsTied() {
        final Long team1Id = 1L;
        final Long team2Id = 2L;

        // No H2H matches played
        final List<MatchResult> matches = List.of();
        final TieBreaker chain = TieBreaker.standardChain(matches, pointsPolicy);

        // Same points (15), same diff (+20), but Team 1 scored more total points (850 vs 820)
        final TeamStanding team1 = new TeamStanding(team1Id, "Team 1", 10, 5, 5, 850, 830, 20, 15);
        final TeamStanding team2 = new TeamStanding(team2Id, "Team 2", 10, 5, 5, 820, 800, 20, 15);

        assertThat(chain.breakTie(team1, team2)).isNegative();
        assertThat(chain.breakTie(team2, team1)).isPositive();
    }

    @Test
    @DisplayName("Zincir: Her şey eşitse takım adı (A -> Z) eşitliği bozar")
    void chainFallsBackToTeamNameWhenAllStatisticsAreIdentical() {
        final Long efesId = 1L;
        final Long fenerId = 2L;

        final List<MatchResult> matches = List.of();
        final TieBreaker chain = TieBreaker.standardChain(matches, pointsPolicy);

        // Identical records across all stats
        final TeamStanding efes = new TeamStanding(efesId, "Anadolu Efes", 10, 5, 5, 850, 830, 20, 15);
        final TeamStanding fener = new TeamStanding(fenerId, "Fenerbahçe Beko", 10, 5, 5, 850, 830, 20, 15);

        // "Anadolu Efes" comes before "Fenerbahçe Beko"
        assertThat(chain.breakTie(efes, fener)).isNegative();
        assertThat(chain.breakTie(fener, efes)).isPositive();
    }

    // --- 6. StandingsComparator Tam Tablo Sıralama Testi ---

    @Test
    @DisplayName("StandingsComparator: Puan ve eşitlik zincirine göre lig tablosunu doğru sıralar")
    void standingsComparatorRanksFullLeagueTableCorrectly() {
        final Long efesId = 1L;
        final Long realId = 2L;
        final Long paoId = 3L;
        final Long fenerId = 4L;
        final Long milanoId = 5L;

        // Real beat Efes in both matches:
        final List<MatchResult> matches = List.of(
                new MatchResult(realId, efesId, 90, 80),
                new MatchResult(efesId, realId, 75, 82)
        );

        final StandingsComparator comparator = StandingsComparator.ofStandard(matches, pointsPolicy);

        // Real & Efes have equal points (18), but Real has H2H advantage -> Real should be #1, Efes #2
        final TeamStanding efes = TeamStanding.of(efesId, "Anadolu Efes", 8, 2, 850, 800, pointsPolicy); // 18 pts
        final TeamStanding real = TeamStanding.of(realId, "Real Madrid", 8, 2, 840, 800, pointsPolicy);  // 18 pts

        // Pao & Fener have equal points (16), no H2H, but Pao has better overall point difference (+30 vs +10) -> Pao #3, Fener #4
        final TeamStanding pao = TeamStanding.of(paoId, "Panathinaikos", 6, 4, 820, 790, pointsPolicy);       // 16 pts, diff +30
        final TeamStanding fener = TeamStanding.of(fenerId, "Fenerbahçe Beko", 6, 4, 800, 790, pointsPolicy); // 16 pts, diff +10

        // Milano has 0 matches played -> #5
        final TeamStanding milano = TeamStanding.zero(milanoId, "EA7 Olimpia Milano"); // 0 pts

        final List<TeamStanding> table = new ArrayList<>(List.of(efes, milano, fener, pao, real));
        table.sort(comparator);

        assertThat(table).extracting(TeamStanding::teamId)
                .containsExactly(realId, efesId, paoId, fenerId, milanoId);
    }

    @Test
    void rejectsInvalidMatchResult() {
        assertThatThrownBy(() -> new MatchResult(1L, 1L, 80, 75))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MatchResult(1L, 2L, -5, 75))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MatchResult(1L, 2L, 80, 80))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

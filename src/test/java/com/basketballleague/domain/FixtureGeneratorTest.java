package com.basketballleague.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class FixtureGeneratorTest {

    private final FixtureGenerator generator = new FixtureGenerator();

    @ParameterizedTest
    @ValueSource(ints = {2, 5, 6, 8})
    void generatesExpectedRoundAndMatchCounts(int teamCount) {
        final List<Long> teams = teamIds(teamCount);
        final int expectedRounds = teamCount % 2 == 0 ? 2 * (teamCount - 1) : 2 * teamCount;

        final Fixture fixture = generator.generate(teams);

        assertThat(fixture.rounds()).hasSize(expectedRounds);
        assertThat(fixture.rounds()).extracting(Round::number)
                .containsExactlyElementsOf(IntStream.rangeClosed(1, expectedRounds).boxed().toList());
        assertThat(allPairings(fixture.rounds())).hasSize(teamCount * (teamCount - 1));
    }

    @Test
    void generatesTenRoundsAndThirtyMatchesForSixTeams() {
        final Fixture fixture = generator.generate(teamIds(6));

        assertThat(fixture.rounds()).hasSize(10);
        assertThat(allPairings(fixture.rounds())).hasSize(30);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 5, 6, 8})
    void schedulesEachTeamAtMostOncePerRoundAndReportsTheBye(int teamCount) {
        final List<Long> teams = teamIds(teamCount);

        final Fixture fixture = generator.generate(teams);

        for (Round round : fixture.rounds()) {
            final List<Long> participants = round.pairings().stream()
                    .flatMap(pairing -> Stream.of(pairing.homeTeamId(), pairing.awayTeamId()))
                    .toList();
            assertThat(round.pairings()).hasSize(teamCount / 2);
            assertThat(participants).doesNotHaveDuplicates().isSubsetOf(teams);
            if (teamCount % 2 == 0) {
                assertThat(round.byeTeamId()).isNull();
                assertThat(participants).containsExactlyInAnyOrderElementsOf(teams);
            } else {
                assertThat(round.byeTeamId()).isIn(teams);
                assertThat(participants).doesNotContain(round.byeTeamId());
                final List<Long> accountedTeams = new ArrayList<>(participants);
                accountedTeams.add(round.byeTeamId());
                assertThat(accountedTeams).containsExactlyInAnyOrderElementsOf(teams);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 5, 6, 8})
    void schedulesEveryOpponentOnceAtHomeAndOnceAway(int teamCount) {
        final List<Long> teams = teamIds(teamCount);

        final Fixture fixture = generator.generate(teams);

        final List<Pairing> expected = teams.stream()
                .flatMap(home -> teams.stream()
                        .filter(away -> !home.equals(away))
                        .map(away -> new Pairing(home, away)))
                .toList();
        assertThat(allPairings(fixture.rounds())).containsExactlyInAnyOrderElementsOf(expected);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 5, 6, 8})
    void reversesVenuesInTheSameRoundAndPairingOrderForTheSecondHalf(int teamCount) {
        final Fixture fixture = generator.generate(teamIds(teamCount));

        final int halfSize = fixture.rounds().size() / 2;
        for (int index = 0; index < halfSize; index++) {
            final Round first = fixture.rounds().get(index);
            final Round second = fixture.rounds().get(index + halfSize);
            final List<Pairing> reversed = first.pairings().stream()
                    .map(pairing -> new Pairing(pairing.awayTeamId(), pairing.homeTeamId()))
                    .toList();
            assertThat(second.pairings()).containsExactlyElementsOf(reversed);
            assertThat(second.byeTeamId()).isEqualTo(first.byeTeamId());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 5, 7, 9})
    void givesEachTeamExactlyOneByePerHalf(int teamCount) {
        final List<Long> teams = teamIds(teamCount);

        final Fixture fixture = generator.generate(teams);

        for (List<Round> half : halves(fixture)) {
            assertThat(half).extracting(Round::byeTeamId).containsExactlyInAnyOrderElementsOf(teams);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 16})
    void balancesHomeAndAwayCountsWithinEachHalf(int teamCount) {
        final List<Long> teams = teamIds(teamCount);

        final Fixture fixture = generator.generate(teams);

        for (List<Round> half : halves(fixture)) {
            final List<Pairing> pairings = allPairings(half);
            for (Long team : teams) {
                final long homeCount = pairings.stream().filter(pairing -> pairing.homeTeamId() == team).count();
                final long awayCount = pairings.stream().filter(pairing -> pairing.awayTeamId() == team).count();
                assertThat(homeCount + awayCount).isEqualTo(teamCount - 1L);
                assertThat(Math.abs(homeCount - awayCount)).isLessThanOrEqualTo(1);
                if (teamCount % 2 != 0) {
                    assertThat(homeCount).isEqualTo(awayCount);
                }
            }
        }
    }

    @Test
    void generatesTheSameFixtureWithoutChangingInputOrder() {
        final List<Long> originalOrder = List.of(90L, 12L, 500L, 3L, Long.MAX_VALUE);
        final List<Long> teams = new ArrayList<>(originalOrder);

        final Fixture first = generator.generate(teams);
        final Fixture second = generator.generate(teams);

        assertThat(second).isEqualTo(first);
        assertThat(teams).containsExactlyElementsOf(originalOrder);
        assertThat(first.rounds().getFirst().byeTeamId()).isEqualTo(Long.MAX_VALUE);
        assertThat(first.rounds().getFirst().pairings())
                .containsExactly(new Pairing(3L, 90L), new Pairing(12L, 500L));
    }

    @Test
    void returnsImmutableListsAndCopiesRecordConstructorInputs() {
        final List<Pairing> pairings = new ArrayList<>(List.of(new Pairing(1L, 2L)));
        final Round round = new Round(1, pairings, null);
        final List<Round> rounds = new ArrayList<>(List.of(round));

        final Fixture fixture = new Fixture(rounds);
        pairings.clear();
        rounds.clear();

        assertThat(fixture.rounds()).containsExactly(round);
        assertThat(round.pairings()).containsExactly(new Pairing(1L, 2L));
        assertThatThrownBy(() -> fixture.rounds().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> round.pairings().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @MethodSource("invalidInputs")
    void rejectsInvalidInputWithDomainException(List<Long> teams) {
        assertThatThrownBy(() -> generator.generate(teams))
                .isInstanceOf(InvalidFixtureInputException.class)
                .isInstanceOf(DomainException.class);
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 500L, Long.MAX_VALUE})
    void rejectsPairingAgainstTheSameTeam(long teamId) {
        assertThatThrownBy(() -> new Pairing(teamId, teamId))
                .isInstanceOf(InvalidFixtureInputException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void rejectsRoundNumbersBelowOne(int number) {
        assertThatThrownBy(() -> new Round(number, List.of(new Pairing(1L, 2L)), null))
                .isInstanceOf(InvalidFixtureInputException.class);
    }

    private static Stream<Arguments> invalidInputs() {
        return Stream.of(
                Arguments.of((Object) null),
                Arguments.of(List.of()),
                Arguments.of(List.of(1L)),
                Arguments.of(List.of(1L, 1L)),
                Arguments.of(List.of(1L, 2L, 1L)),
                Arguments.of(Arrays.asList(1L, null)));
    }

    private static List<Long> teamIds(int count) {
        return LongStream.rangeClosed(1, count).boxed().toList();
    }

    private static List<Pairing> allPairings(List<Round> rounds) {
        return rounds.stream().flatMap(round -> round.pairings().stream()).toList();
    }

    private static List<List<Round>> halves(Fixture fixture) {
        final int halfSize = fixture.rounds().size() / 2;
        return List.of(fixture.rounds().subList(0, halfSize),
                fixture.rounds().subList(halfSize, fixture.rounds().size()));
    }
}

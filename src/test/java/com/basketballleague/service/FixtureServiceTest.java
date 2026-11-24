package com.basketballleague.service;

import static com.basketballleague.support.EntityIds.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.basketballleague.domain.InvalidFixtureInputException;
import com.basketballleague.domain.SeasonAlreadyStartedException;
import com.basketballleague.domain.SeasonNotFoundException;
import com.basketballleague.domain.SeasonStatus;
import com.basketballleague.dto.FixtureResponse;
import com.basketballleague.dto.GameResponse;
import com.basketballleague.dto.RoundResponse;
import com.basketballleague.dto.TeamResponse;
import com.basketballleague.entity.Game;
import com.basketballleague.entity.Season;
import com.basketballleague.entity.Team;
import com.basketballleague.repository.GameRepository;
import com.basketballleague.repository.SeasonRepository;
import com.basketballleague.repository.TeamRepository;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FixtureServiceTest {

    private static final long SEASON_ID = 7L;

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private TeamRepository teamRepository;

    private FixtureService service() {
        return new FixtureService(seasonRepository, gameRepository, teamRepository);
    }

    private static Season seasonWithTeams(int count) {
        final Season season = withId(new Season("2026-27", LocalDate.of(2026, 10, 1)), SEASON_ID);
        LongStream.rangeClosed(1, count).forEach(season::addTeam);
        return season;
    }

    private static List<Team> teams(int count) {
        return LongStream.rangeClosed(1, count)
                .mapToObj(id -> withId(new Team("Takım " + id, "Şehir " + id), id))
                .toList();
    }

    /** Lets the mocked repository assign ids the way the identity column does. */
    private void givenSavedGamesGetIds() {
        final AtomicLong nextId = new AtomicLong(100L);
        when(gameRepository.saveAll(anyList())).thenAnswer(call -> {
            final List<Game> games = call.getArgument(0);
            games.forEach(game -> withId(game, nextId.getAndIncrement()));
            return games;
        });
    }

    private static List<GameResponse> allGames(FixtureResponse fixture) {
        return fixture.rounds().stream().flatMap(round -> round.games().stream()).toList();
    }

    @Test
    void generateFixtureStoresADoubleRoundRobinAndStartsTheSeason() {
        final Season season = seasonWithTeams(4);
        when(seasonRepository.findByIdForUpdate(SEASON_ID)).thenReturn(Optional.of(season));
        when(teamRepository.findByIds(List.of(1L, 2L, 3L, 4L))).thenReturn(teams(4));
        givenSavedGamesGetIds();

        final FixtureResponse fixture = service().generateFixture(SEASON_ID);

        assertThat(season.getStatus()).isEqualTo(SeasonStatus.ACTIVE);
        assertThat(fixture.seasonId()).isEqualTo(SEASON_ID);
        assertThat(fixture.rounds()).extracting(RoundResponse::number).containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(allGames(fixture)).hasSize(12)
                .allSatisfy(game -> assertThat(game.status()).isEqualTo("SCHEDULED"));
        assertThat(fixture.rounds()).allSatisfy(round -> assertThat(round.byeTeam()).isNull());
    }

    @Test
    void generatedGamesCarryTheSeasonAndTheTeamsOfTheirPairing() {
        when(seasonRepository.findByIdForUpdate(SEASON_ID)).thenReturn(Optional.of(seasonWithTeams(2)));
        when(teamRepository.findByIds(List.of(1L, 2L))).thenReturn(teams(2));
        givenSavedGamesGetIds();

        final FixtureResponse fixture = service().generateFixture(SEASON_ID);

        final TeamResponse first = new TeamResponse(1L, "Takım 1", "Şehir 1");
        final TeamResponse second = new TeamResponse(2L, "Takım 2", "Şehir 2");
        assertThat(allGames(fixture)).containsExactly(
                new GameResponse(100L, first, second, "SCHEDULED"),
                new GameResponse(101L, second, first, "SCHEDULED"));
    }

    @Test
    void everyTeamHasExactlyOneByeInEachHalfOfAnOddSizedSeason() {
        when(seasonRepository.findByIdForUpdate(SEASON_ID)).thenReturn(Optional.of(seasonWithTeams(5)));
        when(teamRepository.findByIds(List.of(1L, 2L, 3L, 4L, 5L))).thenReturn(teams(5));
        givenSavedGamesGetIds();

        final FixtureResponse fixture = service().generateFixture(SEASON_ID);

        assertThat(fixture.rounds()).hasSize(10).allSatisfy(round -> {
            assertThat(round.games()).hasSize(2);
            assertThat(round.byeTeam()).isNotNull();
        });
        final Set<Long> firstHalfByes = new HashSet<>();
        fixture.rounds().subList(0, 5).forEach(round -> firstHalfByes.add(round.byeTeam().id()));
        assertThat(firstHalfByes).containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    void secondGenerationIsRejectedWithoutStoringGames() {
        final Season season = seasonWithTeams(4);
        season.activate();
        when(seasonRepository.findByIdForUpdate(SEASON_ID)).thenReturn(Optional.of(season));

        assertThatThrownBy(() -> service().generateFixture(SEASON_ID))
                .isInstanceOf(SeasonAlreadyStartedException.class);
        verify(gameRepository, never()).saveAll(any());
    }

    @Test
    void seasonWithOneTeamStaysADraftAndGetsNoGames() {
        final Season season = seasonWithTeams(1);
        when(seasonRepository.findByIdForUpdate(SEASON_ID)).thenReturn(Optional.of(season));

        assertThatThrownBy(() -> service().generateFixture(SEASON_ID))
                .isInstanceOf(InvalidFixtureInputException.class);
        assertThat(season.getStatus()).isEqualTo(SeasonStatus.DRAFT);
        verify(gameRepository, never()).saveAll(any());
    }

    @Test
    void generateFixtureFailsWithNotFoundForUnknownSeason() {
        when(seasonRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().generateFixture(99L)).isInstanceOf(SeasonNotFoundException.class);
    }

    @Test
    void getFixtureGroupsStoredGamesByRoundAndFindsTheByeTeam() {
        final List<Game> stored = List.of(
                withId(new Game(SEASON_ID, 1, 2L, 3L), 10L),
                withId(new Game(SEASON_ID, 2, 1L, 2L), 11L));
        when(seasonRepository.findById(SEASON_ID)).thenReturn(Optional.of(seasonWithTeams(3)));
        when(seasonRepository.findTeamIds(SEASON_ID)).thenReturn(List.of(3L, 1L, 2L));
        when(teamRepository.findByIds(List.of(3L, 1L, 2L))).thenReturn(teams(3));
        when(gameRepository.findBySeasonId(SEASON_ID)).thenReturn(stored);

        final FixtureResponse fixture = service().getFixture(SEASON_ID);

        assertThat(fixture.rounds()).extracting(RoundResponse::number).containsExactly(1, 2);
        assertThat(fixture.rounds().getFirst().games()).extracting(GameResponse::id).containsExactly(10L);
        assertThat(fixture.rounds().getFirst().byeTeam().id()).isEqualTo(1L);
        assertThat(fixture.rounds().get(1).byeTeam().id()).isEqualTo(3L);
    }

    @Test
    void getFixtureOfADraftSeasonHasNoRounds() {
        when(seasonRepository.findById(SEASON_ID)).thenReturn(Optional.of(seasonWithTeams(2)));
        when(seasonRepository.findTeamIds(SEASON_ID)).thenReturn(List.of(1L, 2L));
        when(teamRepository.findByIds(List.of(1L, 2L))).thenReturn(teams(2));
        when(gameRepository.findBySeasonId(SEASON_ID)).thenReturn(List.of());

        assertThat(service().getFixture(SEASON_ID).rounds()).isEmpty();
    }

    @Test
    void getFixtureFailsWithNotFoundForUnknownSeason() {
        when(seasonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getFixture(99L)).isInstanceOf(SeasonNotFoundException.class);
        verify(gameRepository, never()).findBySeasonId(99L);
    }
}

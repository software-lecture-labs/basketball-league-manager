package com.basketballleague.service;

import static com.basketballleague.support.EntityIds.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.basketballleague.domain.SeasonNotFoundException;
import com.basketballleague.dto.StandingResponse;
import com.basketballleague.entity.Game;
import com.basketballleague.entity.PlayerPoints;
import com.basketballleague.entity.QuarterScore;
import com.basketballleague.entity.Season;
import com.basketballleague.entity.Team;
import com.basketballleague.repository.GameRepository;
import com.basketballleague.repository.SeasonRepository;
import com.basketballleague.repository.TeamRepository;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Ranking rules with real scores are tested in {@code StandingsTableTest}; this class checks the loading and the
 * response mapping, including that a confirmed game's stored score reaches the table.
 */
@ExtendWith(MockitoExtension.class)
class StandingServiceTest {

    private static final long SEASON_ID = 1L;

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private TeamRepository teamRepository;

    private StandingService service() {
        return new StandingService(seasonRepository, gameRepository, teamRepository);
    }

    private void givenSeasonWithTeams(Team... teams) {
        final List<Long> ids = Arrays.stream(teams).map(Team::getId).toList();
        when(seasonRepository.findById(SEASON_ID))
                .thenReturn(Optional.of(new Season("2026-27", LocalDate.of(2026, 10, 1))));
        when(seasonRepository.findTeamIds(SEASON_ID)).thenReturn(ids);
        when(teamRepository.findByIds(ids)).thenReturn(List.of(teams));
        when(gameRepository.findConfirmedBySeasonId(SEASON_ID)).thenReturn(List.of());
    }

    private static Team team(long id, String name) {
        return withId(new Team(name, "Şehir"), id);
    }

    @Test
    void unknownSeasonFailsWithNotFoundBeforeAnyOtherQuery() {
        when(seasonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().calculateStandings(99L)).isInstanceOf(SeasonNotFoundException.class);
        verify(teamRepository, never()).findByIds(anyCollection());
        verify(gameRepository, never()).findConfirmedBySeasonId(anyLong());
    }

    @Test
    void teamsWithoutConfirmedGamesAreListedWithZerosAndRankedByName() {
        givenSeasonWithTeams(team(3L, "Real Madrid"), team(2L, "Panathinaikos"), team(1L, "Anadolu Efes"));

        final List<StandingResponse> standings = service().calculateStandings(SEASON_ID);

        assertThat(standings).containsExactly(
                new StandingResponse(1, 1L, "Anadolu Efes", 0, 0, 0, 0, 0, 0, 0),
                new StandingResponse(2, 2L, "Panathinaikos", 0, 0, 0, 0, 0, 0, 0),
                new StandingResponse(3, 3L, "Real Madrid", 0, 0, 0, 0, 0, 0, 0));
    }

    @Test
    void confirmedGameCountsWithItsStoredScore() {
        givenSeasonWithTeams(team(1L, "Anadolu Efes"), team(9L, "Fenerbahçe Beko"));
        when(gameRepository.findConfirmedBySeasonId(SEASON_ID)).thenReturn(List.of(confirmedGame(1L, 9L)));

        final List<StandingResponse> standings = service().calculateStandings(SEASON_ID);

        assertThat(standings).containsExactly(
                new StandingResponse(1, 1L, "Anadolu Efes", 1, 1, 0, 84, 80, 4, 2),
                new StandingResponse(2, 9L, "Fenerbahçe Beko", 1, 0, 1, 80, 84, -4, 1));
    }

    /** An 84-80 home win, confirmed through the real domain methods. */
    private static Game confirmedGame(long homeTeamId, long awayTeamId) {
        final Game game = withId(new Game(SEASON_ID, 1, homeTeamId, awayTeamId), 10L);
        game.start();
        game.recordQuarters(IntStream.rangeClosed(1, 4).mapToObj(number -> new QuarterScore(number, 21, 20)).toList());
        game.finish();
        game.recordPlayerPoints(List.of(new PlayerPoints(7L, homeTeamId, 84), new PlayerPoints(40L, awayTeamId, 80)));
        game.confirm(game.getVersion());
        return game;
    }

    @Test
    void seasonWithoutTeamsHasAnEmptyTable() {
        givenSeasonWithTeams();

        assertThat(service().calculateStandings(SEASON_ID)).isEmpty();
    }
}

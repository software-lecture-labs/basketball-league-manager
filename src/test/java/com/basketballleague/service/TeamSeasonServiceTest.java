package com.basketballleague.service;

import static com.basketballleague.support.EntityIds.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.basketballleague.domain.SeasonNotFoundException;
import com.basketballleague.domain.TeamNotFoundException;
import com.basketballleague.domain.TeamNotInSeasonException;
import com.basketballleague.dto.SeasonResponse;
import com.basketballleague.dto.StandingResponse;
import com.basketballleague.dto.TeamRecentGameResponse;
import com.basketballleague.dto.TeamResponse;
import com.basketballleague.dto.TeamSeasonOverviewResponse;
import com.basketballleague.entity.Game;
import com.basketballleague.entity.PlayerPoints;
import com.basketballleague.entity.QuarterScore;
import com.basketballleague.entity.Season;
import com.basketballleague.entity.Team;
import com.basketballleague.repository.GameRepository;
import com.basketballleague.repository.SeasonRepository;
import com.basketballleague.repository.TeamRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamSeasonServiceTest {

    private static final long TEAM_ID = 1L;
    private static final long OPPONENT_ID = 2L;
    private static final long SEASON_ID = 7L;
    private static final LocalDate START_DATE = LocalDate.of(2026, 10, 1);

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private StandingService standingService;

    private TeamSeasonService service() {
        return new TeamSeasonService(teamRepository, seasonRepository, gameRepository, standingService);
    }

    private static Team team(long id, String name) {
        return withId(new Team(name, "İstanbul"), id);
    }

    @Test
    void listSeasonsReturnsOnlySeasonsContainingTheTeam() {
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team(TEAM_ID, "Anadolu Efes")));
        when(seasonRepository.findByTeamId(TEAM_ID))
                .thenReturn(List.of(withId(new Season("2026-27", START_DATE), SEASON_ID)));

        assertThat(service().listSeasons(TEAM_ID))
                .containsExactly(new SeasonResponse(SEASON_ID, "2026-27", START_DATE, "DRAFT"));
    }

    @Test
    void listSeasonsRejectsAnUnknownTeam() {
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().listSeasons(99L)).isInstanceOf(TeamNotFoundException.class);
        verify(seasonRepository, never()).findByTeamId(99L);
    }

    @Test
    void overviewReturnsStandingsAndTheMostRecentConfirmedGames() {
        final Team home = team(TEAM_ID, "Anadolu Efes");
        final Team away = team(OPPONENT_ID, "Fenerbahçe");
        final Season season = withId(new Season("2026-27", START_DATE), SEASON_ID);
        final StandingResponse summary =
                new StandingResponse(2, TEAM_ID, home.getName(), 5, 3, 2, 420, 399, 21, 8);
        final Game game = confirmedGame();
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(home));
        when(seasonRepository.findById(SEASON_ID)).thenReturn(Optional.of(season));
        when(seasonRepository.findTeamIds(SEASON_ID)).thenReturn(List.of(TEAM_ID, OPPONENT_ID));
        when(standingService.calculateStandings(SEASON_ID)).thenReturn(List.of(summary));
        when(gameRepository.findRecentConfirmedBySeasonAndTeamId(SEASON_ID, TEAM_ID, 5)).thenReturn(List.of(game));
        when(teamRepository.findByIds(Set.of(TEAM_ID, OPPONENT_ID))).thenReturn(List.of(home, away));

        final TeamSeasonOverviewResponse overview = service().getOverview(TEAM_ID, SEASON_ID);

        assertThat(overview.season()).isEqualTo(new SeasonResponse(SEASON_ID, "2026-27", START_DATE, "DRAFT"));
        assertThat(overview.summary()).isEqualTo(summary);
        assertThat(overview.lastFiveGames()).containsExactly(new TeamRecentGameResponse(
                81L, 4, new TeamResponse(TEAM_ID, home.getName(), home.getCity()),
                new TeamResponse(OPPONENT_ID, away.getName(), away.getCity()), 80, 75));
    }

    @Test
    void overviewRejectsASeasonTheTeamDoesNotParticipateIn() {
        final Team team = team(TEAM_ID, "Anadolu Efes");
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
        when(seasonRepository.findById(SEASON_ID))
                .thenReturn(Optional.of(withId(new Season("2026-27", START_DATE), SEASON_ID)));
        when(seasonRepository.findTeamIds(SEASON_ID)).thenReturn(List.of(OPPONENT_ID));

        assertThatThrownBy(() -> service().getOverview(TEAM_ID, SEASON_ID))
                .isInstanceOf(TeamNotInSeasonException.class);
        verify(standingService, never()).calculateStandings(SEASON_ID);
    }

    @Test
    void overviewSupportsASeasonWithNoConfirmedGames() {
        final Team team = team(TEAM_ID, "Anadolu Efes");
        final Season season = withId(new Season("2026-27", START_DATE), SEASON_ID);
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
        when(seasonRepository.findById(SEASON_ID)).thenReturn(Optional.of(season));
        when(seasonRepository.findTeamIds(SEASON_ID)).thenReturn(List.of(TEAM_ID));
        when(standingService.calculateStandings(SEASON_ID)).thenReturn(
                List.of(new StandingResponse(1, TEAM_ID, team.getName(), 0, 0, 0, 0, 0, 0, 0)));
        when(gameRepository.findRecentConfirmedBySeasonAndTeamId(SEASON_ID, TEAM_ID, 5)).thenReturn(List.of());
        when(teamRepository.findByIds(Set.of())).thenReturn(List.of());

        assertThat(service().getOverview(TEAM_ID, SEASON_ID).lastFiveGames()).isEmpty();
    }

    @Test
    void overviewRejectsAnUnknownSeason() {
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team(TEAM_ID, "Anadolu Efes")));
        when(seasonRepository.findById(SEASON_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getOverview(TEAM_ID, SEASON_ID))
                .isInstanceOf(SeasonNotFoundException.class);
        verify(seasonRepository, never()).findTeamIds(SEASON_ID);
    }

    private static Game confirmedGame() {
        final Game game = withId(new Game(SEASON_ID, 4, TEAM_ID, OPPONENT_ID), 81L);
        game.start();
        game.recordQuarters(List.of(
                new QuarterScore(1, 20, 18),
                new QuarterScore(2, 19, 17),
                new QuarterScore(3, 21, 20),
                new QuarterScore(4, 20, 20)));
        game.finish();
        game.recordPlayerPoints(List.of(
                new PlayerPoints(10L, TEAM_ID, 80),
                new PlayerPoints(20L, OPPONENT_ID, 75)));
        game.confirm(0L);
        return game;
    }
}

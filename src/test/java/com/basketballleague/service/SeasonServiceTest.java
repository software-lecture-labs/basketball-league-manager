package com.basketballleague.service;

import static com.basketballleague.support.EntityIds.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.basketballleague.domain.SeasonAlreadyStartedException;
import com.basketballleague.domain.SeasonNameTakenException;
import com.basketballleague.domain.SeasonNotFoundException;
import com.basketballleague.domain.TeamAlreadyInSeasonException;
import com.basketballleague.domain.TeamNotFoundException;
import com.basketballleague.dto.AddSeasonTeamRequest;
import com.basketballleague.dto.CreateSeasonRequest;
import com.basketballleague.dto.PageResponse;
import com.basketballleague.dto.SeasonResponse;
import com.basketballleague.dto.TeamResponse;
import com.basketballleague.entity.Season;
import com.basketballleague.entity.Team;
import com.basketballleague.repository.SeasonRepository;
import com.basketballleague.repository.TeamRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SeasonServiceTest {

    private static final long SEASON_ID = 3L;
    private static final long TEAM_ID = 8L;
    private static final LocalDate START = LocalDate.of(2026, 10, 1);
    private static final String NAME = "2026-27 Demo Sezonu";

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private TeamRepository teamRepository;

    private SeasonService service() {
        return new SeasonService(seasonRepository, teamRepository);
    }

    private static Season season() {
        return withId(new Season(NAME, START), SEASON_ID);
    }

    private static Team team() {
        return withId(new Team("Anadolu Efes", "İstanbul"), TEAM_ID);
    }

    @Test
    void listSeasonsAsksRepositoryForTheRequestedPageAndReportsTheTotal() {
        when(seasonRepository.findPage(40, 20)).thenReturn(List.of(season()));
        when(seasonRepository.count()).thenReturn(41L);

        final PageResponse<SeasonResponse> page = service().listSeasons(2, 20);

        assertThat(page.items()).containsExactly(new SeasonResponse(SEASON_ID, NAME, START, "DRAFT"));
        assertThat(page.totalItems()).isEqualTo(41L);
    }

    @Test
    void getSeasonFailsWithNotFoundForUnknownId() {
        when(seasonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getSeason(99L)).isInstanceOf(SeasonNotFoundException.class);
    }

    @Test
    void createSeasonSavesADraftSeasonWithItsStrippedName() {
        when(seasonRepository.findByName(NAME)).thenReturn(Optional.empty());
        when(seasonRepository.save(any(Season.class))).thenAnswer(call -> withId(call.getArgument(0), 1L));

        final SeasonResponse created = service().createSeason(new CreateSeasonRequest("  " + NAME + " ", START));

        assertThat(created).isEqualTo(new SeasonResponse(1L, NAME, START, "DRAFT"));
    }

    @Test
    void createSeasonRejectsADuplicateNameWithoutSaving() {
        when(seasonRepository.findByName(NAME)).thenReturn(Optional.of(season()));

        assertThatThrownBy(() -> service().createSeason(new CreateSeasonRequest(NAME, START)))
                .isInstanceOf(SeasonNameTakenException.class);
        verify(seasonRepository, never()).save(any());
    }

    @Test
    void listTeamsReturnsTheTeamsOfTheSeasonInTheRepositoryOrder() {
        final Team zalgiris = withId(new Team("Žalgiris", "Kaunas"), 2L);
        when(seasonRepository.findById(SEASON_ID)).thenReturn(Optional.of(season()));
        when(seasonRepository.findTeamIds(SEASON_ID)).thenReturn(List.of(2L, TEAM_ID));
        when(teamRepository.findByIds(List.of(2L, TEAM_ID))).thenReturn(List.of(team(), zalgiris));

        assertThat(service().listTeams(SEASON_ID)).containsExactly(
                new TeamResponse(TEAM_ID, "Anadolu Efes", "İstanbul"),
                new TeamResponse(2L, "Žalgiris", "Kaunas"));
    }

    @Test
    void listTeamsFailsWithNotFoundForUnknownSeason() {
        when(seasonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().listTeams(99L)).isInstanceOf(SeasonNotFoundException.class);
        verify(teamRepository, never()).findByIds(any());
    }

    @Test
    void addTeamLocksTheSeasonAndAddsTheTeam() {
        final Season season = season();
        when(seasonRepository.findByIdForUpdate(SEASON_ID)).thenReturn(Optional.of(season));
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team()));

        final TeamResponse added = service().addTeam(SEASON_ID, new AddSeasonTeamRequest(TEAM_ID));

        assertThat(added).isEqualTo(new TeamResponse(TEAM_ID, "Anadolu Efes", "İstanbul"));
        assertThat(season.getTeamIds()).containsExactly(TEAM_ID);
        verify(seasonRepository, never()).findById(anyLong());
    }

    @Test
    void addTeamFailsWithNotFoundForUnknownSeason() {
        when(seasonRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().addTeam(99L, new AddSeasonTeamRequest(TEAM_ID)))
                .isInstanceOf(SeasonNotFoundException.class);
    }

    @Test
    void addTeamFailsWithNotFoundForUnknownTeamAndLeavesTheSeasonUnchanged() {
        final Season season = season();
        when(seasonRepository.findByIdForUpdate(SEASON_ID)).thenReturn(Optional.of(season));
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().addTeam(SEASON_ID, new AddSeasonTeamRequest(99L)))
                .isInstanceOf(TeamNotFoundException.class);
        assertThat(season.getTeamIds()).isEmpty();
    }

    @Test
    void addTeamPassesTheSeasonRuleViolationsThrough() {
        final Season season = season();
        season.addTeam(TEAM_ID);
        when(seasonRepository.findByIdForUpdate(SEASON_ID)).thenReturn(Optional.of(season));
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team()));

        assertThatThrownBy(() -> service().addTeam(SEASON_ID, new AddSeasonTeamRequest(TEAM_ID)))
                .isInstanceOf(TeamAlreadyInSeasonException.class);
    }

    @Test
    void addTeamRejectsAStartedSeason() {
        final Season season = season();
        season.addTeam(1L);
        season.addTeam(2L);
        season.activate();
        when(seasonRepository.findByIdForUpdate(SEASON_ID)).thenReturn(Optional.of(season));
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team()));

        assertThatThrownBy(() -> service().addTeam(SEASON_ID, new AddSeasonTeamRequest(TEAM_ID)))
                .isInstanceOf(SeasonAlreadyStartedException.class);
    }
}

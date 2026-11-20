package com.basketballleague.service;

import static com.basketballleague.support.EntityIds.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.basketballleague.domain.ErrorCode;
import com.basketballleague.domain.JerseyNumberTakenException;
import com.basketballleague.domain.TeamNameTakenException;
import com.basketballleague.domain.TeamNotFoundException;
import com.basketballleague.dto.CreatePlayerRequest;
import com.basketballleague.dto.CreateTeamRequest;
import com.basketballleague.dto.PageResponse;
import com.basketballleague.dto.PlayerResponse;
import com.basketballleague.dto.TeamResponse;
import com.basketballleague.entity.Player;
import com.basketballleague.entity.Position;
import com.basketballleague.entity.Team;
import com.basketballleague.repository.PlayerRepository;
import com.basketballleague.repository.TeamRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    private static final long TEAM_ID = 4L;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private PlayerRepository playerRepository;

    private TeamService service() {
        return new TeamService(teamRepository, playerRepository);
    }

    private static Team team() {
        return withId(new Team("Fenerbahçe Beko", "İstanbul"), TEAM_ID);
    }

    @Test
    void listTeamsAsksRepositoryForTheRequestedPageAndReportsTheTotal() {
        when(teamRepository.findPage(40, 20)).thenReturn(List.of(team()));
        when(teamRepository.count()).thenReturn(41L);

        final PageResponse<TeamResponse> page = service().listTeams(2, 20);

        assertThat(page.items()).containsExactly(new TeamResponse(TEAM_ID, "Fenerbahçe Beko", "İstanbul"));
        assertThat(page.page()).isEqualTo(2);
        assertThat(page.size()).isEqualTo(20);
        assertThat(page.totalItems()).isEqualTo(41L);
    }

    @Test
    void getTeamFailsWithNotFoundForUnknownId() {
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getTeam(99L))
                .isInstanceOf(TeamNotFoundException.class)
                .extracting("code").isEqualTo(ErrorCode.TEAM_NOT_FOUND);
    }

    @Test
    void createTeamSavesAndReturnsTheTeamWithItsNewId() {
        when(teamRepository.findByName("Anadolu Efes")).thenReturn(Optional.empty());
        when(teamRepository.save(any(Team.class))).thenAnswer(call -> withId(call.getArgument(0), 1L));

        final TeamResponse created = service().createTeam(new CreateTeamRequest("  Anadolu Efes ", "İstanbul"));

        assertThat(created).isEqualTo(new TeamResponse(1L, "Anadolu Efes", "İstanbul"));
    }

    @Test
    void createTeamRejectsADuplicateNameWithoutSaving() {
        when(teamRepository.findByName("Anadolu Efes")).thenReturn(Optional.of(team()));

        assertThatThrownBy(() -> service().createTeam(new CreateTeamRequest("Anadolu Efes ", "İstanbul")))
                .isInstanceOf(TeamNameTakenException.class);
        verify(teamRepository, never()).save(any());
    }

    @Test
    void listPlayersReturnsThePageOrderedByTheRepository() {
        final Team team = team();
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
        when(playerRepository.findPageByTeamId(TEAM_ID, 0, 20))
                .thenReturn(List.of(withId(new Player(team, "Shane", "Larkin", 3, Position.PG), 7L)));
        when(playerRepository.countByTeamId(TEAM_ID)).thenReturn(1L);

        final PageResponse<PlayerResponse> page = service().listPlayers(TEAM_ID, 0, 20);

        assertThat(page.items()).containsExactly(new PlayerResponse(7L, "Shane", "Larkin", 3, "PG"));
        assertThat(page.totalItems()).isEqualTo(1L);
    }

    @Test
    void listPlayersFailsWithNotFoundBeforeQueryingPlayers() {
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().listPlayers(99L, 0, 20)).isInstanceOf(TeamNotFoundException.class);
        verify(playerRepository, never()).findPageByTeamId(anyLong(), anyInt(), anyInt());
    }

    @Test
    void addPlayerSavesThePlayerOnTheTeam() {
        final Team team = team();
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
        when(playerRepository.existsByTeamIdAndJerseyNumber(TEAM_ID, 3)).thenReturn(false);
        when(playerRepository.save(any(Player.class))).thenAnswer(call -> withId(call.getArgument(0), 7L));

        final CreatePlayerRequest request = new CreatePlayerRequest("Shane", "Larkin", 3, "PG");

        final PlayerResponse created = service().addPlayer(TEAM_ID, request);

        assertThat(created).isEqualTo(new PlayerResponse(7L, "Shane", "Larkin", 3, "PG"));
        final ArgumentCaptor<Player> saved = ArgumentCaptor.forClass(Player.class);
        verify(playerRepository).save(saved.capture());
        assertThat(saved.getValue().getTeam()).isSameAs(team);
        assertThat(saved.getValue().getPosition()).isEqualTo(Position.PG);
    }

    @Test
    void addPlayerRejectsATakenJerseyNumberWithoutSaving() {
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team()));
        when(playerRepository.existsByTeamIdAndJerseyNumber(TEAM_ID, 3)).thenReturn(true);

        assertThatThrownBy(() -> service().addPlayer(TEAM_ID, new CreatePlayerRequest("Rodrigue", "Beaubois", 3, "SG")))
                .isInstanceOf(JerseyNumberTakenException.class)
                .extracting("code").isEqualTo(ErrorCode.JERSEY_NUMBER_TAKEN);
        verify(playerRepository, never()).save(any());
    }

    @Test
    void addPlayerFailsWithNotFoundForUnknownTeam() {
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().addPlayer(99L, new CreatePlayerRequest("Shane", "Larkin", 3, "PG")))
                .isInstanceOf(TeamNotFoundException.class);
        verify(playerRepository, never()).save(any());
    }
}

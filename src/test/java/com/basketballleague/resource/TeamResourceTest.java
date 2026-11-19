package com.basketballleague.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.basketballleague.dto.CreatePlayerRequest;
import com.basketballleague.dto.CreateTeamRequest;
import com.basketballleague.dto.PageResponse;
import com.basketballleague.dto.PlayerResponse;
import com.basketballleague.dto.TeamResponse;
import com.basketballleague.service.TeamService;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamResourceTest {

    private static final TeamResponse TEAM = new TeamResponse(1L, "Anadolu Efes", "İstanbul");
    private static final PlayerResponse PLAYER = new PlayerResponse(7L, "Shane", "Larkin", 3, "PG");

    @Mock
    private TeamService teamService;

    @Mock
    private UriInfo uriInfo;

    private TeamResource resource() {
        return new TeamResource(teamService);
    }

    @Test
    void listTeamsReturnsThePageFromTheService() {
        final PageResponse<TeamResponse> page = new PageResponse<>(List.of(TEAM), 0, 20, 1L);
        when(teamService.listTeams(0, 20)).thenReturn(page);

        assertThat(resource().listTeams(0, 20)).isSameAs(page);
    }

    @Test
    void createTeamAnswers201WithTheNewTeamsAddress() {
        final CreateTeamRequest request = new CreateTeamRequest("Anadolu Efes", "İstanbul");
        when(teamService.createTeam(request)).thenReturn(TEAM);
        when(uriInfo.getAbsolutePathBuilder())
                .thenReturn(UriBuilder.fromUri("http://localhost:8080/basketball-league/api/teams"));

        final Response response = resource().createTeam(request, uriInfo);

        assertThat(response.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
        assertThat(response.getLocation())
                .isEqualTo(URI.create("http://localhost:8080/basketball-league/api/teams/1"));
        assertThat(response.getEntity()).isEqualTo(TEAM);
    }

    @Test
    void listPlayersReturnsThePageFromTheService() {
        final PageResponse<PlayerResponse> page = new PageResponse<>(List.of(PLAYER), 1, 5, 6L);
        when(teamService.listPlayers(1L, 1, 5)).thenReturn(page);

        assertThat(resource().listPlayers(1L, 1, 5)).isSameAs(page);
    }

    @Test
    void addPlayerAnswers201WithThePlayerAndNoLocation() {
        final CreatePlayerRequest request = new CreatePlayerRequest("Shane", "Larkin", 3, "PG");
        when(teamService.addPlayer(1L, request)).thenReturn(PLAYER);

        final Response response = resource().addPlayer(1L, request);

        assertThat(response.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
        assertThat(response.getLocation()).isNull();
        assertThat(response.getEntity()).isEqualTo(PLAYER);
    }
}

package com.basketballleague.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.basketballleague.dto.AddSeasonTeamRequest;
import com.basketballleague.dto.CreateSeasonRequest;
import com.basketballleague.dto.PageResponse;
import com.basketballleague.dto.SeasonResponse;
import com.basketballleague.dto.TeamResponse;
import com.basketballleague.service.SeasonService;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SeasonResourceTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 1);
    private static final SeasonResponse SEASON = new SeasonResponse(1L, "2026-27 Demo Sezonu", START, "DRAFT");
    private static final TeamResponse TEAM = new TeamResponse(4L, "Anadolu Efes", "İstanbul");

    @Mock
    private SeasonService seasonService;

    @Mock
    private UriInfo uriInfo;

    private SeasonResource resource() {
        return new SeasonResource(seasonService);
    }

    @Test
    void listSeasonsReturnsThePageFromTheService() {
        final PageResponse<SeasonResponse> page = new PageResponse<>(List.of(SEASON), 0, 20, 1L);
        when(seasonService.listSeasons(0, 20)).thenReturn(page);

        assertThat(resource().listSeasons(0, 20)).isSameAs(page);
    }

    @Test
    void getSeasonReturnsTheSeasonFromTheService() {
        when(seasonService.getSeason(1L)).thenReturn(SEASON);

        assertThat(resource().getSeason(1L)).isEqualTo(SEASON);
    }

    @Test
    void createSeasonAnswers201WithTheNewSeasonsAddress() {
        final CreateSeasonRequest request = new CreateSeasonRequest("2026-27 Demo Sezonu", START);
        when(seasonService.createSeason(request)).thenReturn(SEASON);
        when(uriInfo.getAbsolutePathBuilder())
                .thenReturn(UriBuilder.fromUri("http://localhost:8080/basketball-league/api/seasons"));

        final Response response = resource().createSeason(request, uriInfo);

        assertThat(response.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
        assertThat(response.getLocation())
                .isEqualTo(URI.create("http://localhost:8080/basketball-league/api/seasons/1"));
        assertThat(response.getEntity()).isEqualTo(SEASON);
    }

    @Test
    void listTeamsReturnsTheTeamsFromTheService() {
        when(seasonService.listTeams(1L)).thenReturn(List.of(TEAM));

        assertThat(resource().listTeams(1L)).containsExactly(TEAM);
    }

    @Test
    void addTeamAnswers201WithTheTeamAndNoLocation() {
        final AddSeasonTeamRequest request = new AddSeasonTeamRequest(4L);
        when(seasonService.addTeam(1L, request)).thenReturn(TEAM);

        final Response response = resource().addTeam(1L, request);

        assertThat(response.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
        assertThat(response.getLocation()).isNull();
        assertThat(response.getEntity()).isEqualTo(TEAM);
    }
}

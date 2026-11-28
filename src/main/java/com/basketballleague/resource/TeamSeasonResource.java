package com.basketballleague.resource;

import com.basketballleague.dto.SeasonResponse;
import com.basketballleague.dto.TeamSeasonOverviewResponse;
import com.basketballleague.service.TeamSeasonService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * Season history for one team under {@code /api/teams/{teamId}/seasons}.
 */
@Path("teams/{teamId}/seasons")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class TeamSeasonResource {

    private static final String TEAM_ID = "teamId";
    private static final String SEASON_ID = "seasonId";

    private final TeamSeasonService teamSeasonService;

    /** Required by CDI to create the client proxy; never used to serve calls. */
    protected TeamSeasonResource() {
        this.teamSeasonService = null;
    }

    @Inject
    public TeamSeasonResource(TeamSeasonService teamSeasonService) {
        this.teamSeasonService = teamSeasonService;
    }

    @GET
    public List<SeasonResponse> listSeasons(@PathParam(TEAM_ID) @Positive long teamId) {
        return teamSeasonService.listSeasons(teamId);
    }

    @GET
    @Path("{seasonId}/overview")
    public TeamSeasonOverviewResponse getOverview(
            @PathParam(TEAM_ID) @Positive long teamId,
            @PathParam(SEASON_ID) @Positive long seasonId) {
        return teamSeasonService.getOverview(teamId, seasonId);
    }
}

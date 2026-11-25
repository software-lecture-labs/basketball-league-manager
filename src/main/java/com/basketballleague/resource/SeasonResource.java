package com.basketballleague.resource;

import static com.basketballleague.resource.PageLimits.DEFAULT_PAGE_SIZE;
import static com.basketballleague.resource.PageLimits.MAX_PAGE;
import static com.basketballleague.resource.PageLimits.MAX_PAGE_SIZE;

import com.basketballleague.dto.AddSeasonTeamRequest;
import com.basketballleague.dto.CreateSeasonRequest;
import com.basketballleague.dto.PageResponse;
import com.basketballleague.dto.SeasonResponse;
import com.basketballleague.dto.TeamResponse;
import com.basketballleague.dto.StandingResponse;
import com.basketballleague.service.SeasonService;
import com.basketballleague.service.StandingService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

/**
 * Seasons and their teams under {@code /api/seasons}. The contract is in {@code docs/api.md}.
 */
@Path("seasons")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SeasonResource {

    private static final String SEASON_ID = "seasonId";

    private final SeasonService seasonService;
    private final StandingService standingService;

    /** Required by CDI to create the client proxy; never used to serve calls. */
    protected SeasonResource() {
        this.seasonService = null;
        this.standingService = null;
    }

    @Inject
    public SeasonResource(SeasonService seasonService, StandingService standingService) {
        this.seasonService = seasonService;
        this.standingService = standingService;
    }

    @GET
    public PageResponse<SeasonResponse> listSeasons(
            @QueryParam("page") @DefaultValue("0") @Min(0) @Max(MAX_PAGE) int page,
            @QueryParam("size") @DefaultValue(DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return seasonService.listSeasons(page, size);
    }

    @GET
    @Path("{seasonId}")
    public SeasonResponse getSeason(@PathParam(SEASON_ID) @Positive long seasonId) {
        return seasonService.getSeason(seasonId);
    }

    @POST
    public Response createSeason(@NotNull @Valid CreateSeasonRequest request, @Context UriInfo uriInfo) {
        final SeasonResponse created = seasonService.createSeason(request);
        final URI location = uriInfo.getAbsolutePathBuilder().path(Long.toString(created.id())).build();
        return Response.created(location).entity(created).build();
    }

    /**
     * Not paged: a season has at most 20 teams (see docs/api.md).
     */
    @GET
    @Path("{seasonId}/teams")
    public List<TeamResponse> listTeams(@PathParam(SEASON_ID) @Positive long seasonId) {
        return seasonService.listTeams(seasonId);
    }

    /**
     * No {@code Location} header: a season membership does not have its own address (see docs/api.md).
     */
    @POST
    @Path("{seasonId}/teams")
    public Response addTeam(@PathParam(SEASON_ID) @Positive long seasonId,
            @NotNull @Valid AddSeasonTeamRequest request) {
        final TeamResponse added = seasonService.addTeam(seasonId, request);
        return Response.status(Response.Status.CREATED).entity(added).build();
    }

    /**
     * Returns the current standings for the given season, computed from CONFIRMED games.
     *
     * <p>Not paged: a season has at most 20 teams (see docs/api.md).
     * <p>Scores (and therefore non-zero stats) will appear once Osman's 3.2 (score entry) is merged.
     *
     * @see StandingService#calculateStandings(long)
     */
    @GET
    @Path("{seasonId}/standings")
    public List<StandingResponse> getStandings(@PathParam(SEASON_ID) @Positive long seasonId) {
        return standingService.calculateStandings(seasonId);
    }
}

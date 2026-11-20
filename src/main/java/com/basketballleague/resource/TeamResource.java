package com.basketballleague.resource;

import com.basketballleague.dto.CreatePlayerRequest;
import com.basketballleague.dto.CreateTeamRequest;
import com.basketballleague.dto.PageResponse;
import com.basketballleague.dto.PlayerResponse;
import com.basketballleague.dto.TeamResponse;
import com.basketballleague.service.TeamService;
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

/**
 * Teams and their players under {@code /api/teams}. The contract is in {@code docs/api.md}.
 */
@Path("teams")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TeamResource {

    static final String DEFAULT_PAGE_SIZE = "20";
    static final int MAX_PAGE_SIZE = 100;
    /** Keeps {@code page * size} inside an int, the type JPA uses for the first result. */
    static final int MAX_PAGE = Integer.MAX_VALUE / MAX_PAGE_SIZE;

    private static final String TEAM_ID = "teamId";

    private final TeamService teamService;

    /** Required by CDI to create the client proxy; never used to serve calls. */
    protected TeamResource() {
        this.teamService = null;
    }

    @Inject
    public TeamResource(TeamService teamService) {
        this.teamService = teamService;
    }

    @GET
    public PageResponse<TeamResponse> listTeams(
            @QueryParam("page") @DefaultValue("0") @Min(0) @Max(MAX_PAGE) int page,
            @QueryParam("size") @DefaultValue(DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return teamService.listTeams(page, size);
    }

    @GET
    @Path("{teamId}")
    public TeamResponse getTeam(@PathParam(TEAM_ID) @Positive long teamId) {
        return teamService.getTeam(teamId);
    }

    @POST
    public Response createTeam(@NotNull @Valid CreateTeamRequest request, @Context UriInfo uriInfo) {
        final TeamResponse created = teamService.createTeam(request);
        final URI location = uriInfo.getAbsolutePathBuilder().path(Long.toString(created.id())).build();
        return Response.created(location).entity(created).build();
    }

    @GET
    @Path("{teamId}/players")
    public PageResponse<PlayerResponse> listPlayers(
            @PathParam(TEAM_ID) @Positive long teamId,
            @QueryParam("page") @DefaultValue("0") @Min(0) @Max(MAX_PAGE) int page,
            @QueryParam("size") @DefaultValue(DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return teamService.listPlayers(teamId, page, size);
    }

    /**
     * No {@code Location} header: players do not have their own address yet (see docs/api.md).
     */
    @POST
    @Path("{teamId}/players")
    public Response addPlayer(@PathParam(TEAM_ID) @Positive long teamId,
            @NotNull @Valid CreatePlayerRequest request) {
        final PlayerResponse created = teamService.addPlayer(teamId, request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }
}

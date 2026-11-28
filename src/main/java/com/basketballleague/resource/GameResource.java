package com.basketballleague.resource;

import com.basketballleague.dto.GameDetailResponse;
import com.basketballleague.dto.GameVersionRequest;
import com.basketballleague.dto.RecordPlayerPointsRequest;
import com.basketballleague.dto.RecordQuartersRequest;
import com.basketballleague.service.GameService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Result entry and confirmation of a game under {@code /api/games}. Every change carries the version the client
 * last read and returns the updated game (ADR-0004). The contract is in {@code docs/api.md}.
 */
@Path("games/{gameId}")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GameResource {

    private static final String GAME_ID = "gameId";

    private final GameService gameService;

    /** Required by CDI to create the client proxy; never used to serve calls. */
    protected GameResource() {
        this.gameService = null;
    }

    @Inject
    public GameResource(GameService gameService) {
        this.gameService = gameService;
    }

    @GET
    public GameDetailResponse getGame(@PathParam(GAME_ID) @Positive long gameId) {
        return gameService.getGame(gameId);
    }

    @POST
    @Path("start")
    public GameDetailResponse start(@PathParam(GAME_ID) @Positive long gameId,
            @NotNull @Valid GameVersionRequest request) {
        return gameService.start(gameId, request.version());
    }

    @PUT
    @Path("quarters")
    public GameDetailResponse recordQuarters(@PathParam(GAME_ID) @Positive long gameId,
            @NotNull @Valid RecordQuartersRequest request) {
        return gameService.recordQuarters(gameId, request);
    }

    @PUT
    @Path("stats")
    public GameDetailResponse recordPlayerPoints(@PathParam(GAME_ID) @Positive long gameId,
            @NotNull @Valid RecordPlayerPointsRequest request) {
        return gameService.recordPlayerPoints(gameId, request);
    }

    @POST
    @Path("finish")
    public GameDetailResponse finish(@PathParam(GAME_ID) @Positive long gameId,
            @NotNull @Valid GameVersionRequest request) {
        return gameService.finish(gameId, request.version());
    }

    @POST
    @Path("reopen")
    public GameDetailResponse reopen(@PathParam(GAME_ID) @Positive long gameId,
            @NotNull @Valid GameVersionRequest request) {
        return gameService.reopen(gameId, request.version());
    }

    @POST
    @Path("confirm")
    public GameDetailResponse confirm(@PathParam(GAME_ID) @Positive long gameId,
            @NotNull @Valid GameVersionRequest request) {
        return gameService.confirm(gameId, request.version());
    }
}

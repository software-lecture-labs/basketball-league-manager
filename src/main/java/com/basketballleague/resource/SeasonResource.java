package com.basketballleague.resource;

import com.basketballleague.dto.StandingResponse;
import com.basketballleague.service.StandingService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * Seasons and their calculated standings under {@code /api/seasons}.
 */
@Path("seasons")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SeasonResource {

    private final StandingService standingService;

    /** Required by CDI to create the client proxy; never used to serve calls. */
    protected SeasonResource() {
        this.standingService = null;
    }

    @Inject
    public SeasonResource(StandingService standingService) {
        this.standingService = standingService;
    }

    @GET
    @Path("{seasonId}/standings")
    public List<StandingResponse> getStandings(
            @PathParam("seasonId") @NotNull @Positive long seasonId) {
        return standingService.calculateStandings(seasonId);
    }
}

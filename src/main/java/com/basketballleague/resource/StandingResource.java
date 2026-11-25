package com.basketballleague.resource;

import com.basketballleague.dto.StandingResponse;
import com.basketballleague.service.StandingService;
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
 * The league table of a season under {@code /api/seasons/{id}/standings}. A separate class from
 * {@link SeasonResource}, like {@link FixtureResource}, because standings have their own service.
 * The contract is in {@code docs/api.md}.
 */
@Path("seasons/{seasonId}/standings")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class StandingResource {

    private final StandingService standingService;

    /** Required by CDI to create the client proxy; never used to serve calls. */
    protected StandingResource() {
        this.standingService = null;
    }

    @Inject
    public StandingResource(StandingService standingService) {
        this.standingService = standingService;
    }

    /**
     * Not paged: a season has at most 20 teams (see docs/api.md).
     */
    @GET
    public List<StandingResponse> getStandings(@PathParam("seasonId") @Positive long seasonId) {
        return standingService.calculateStandings(seasonId);
    }
}

package com.basketballleague.resource;

import com.basketballleague.dto.FixtureResponse;
import com.basketballleague.service.FixtureService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * The fixture of a season under {@code /api/seasons/{id}/fixtures}. A separate class from {@link SeasonResource}
 * because fixtures have their own service; Jakarta REST picks this class for the longer matching path.
 * The contract is in {@code docs/api.md}.
 */
@Path("seasons/{seasonId}/fixtures")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class FixtureResource {

    private final FixtureService fixtureService;

    /** Required by CDI to create the client proxy; never used to serve calls. */
    protected FixtureResource() {
        this.fixtureService = null;
    }

    @Inject
    public FixtureResource(FixtureService fixtureService) {
        this.fixtureService = fixtureService;
    }

    @GET
    public FixtureResponse getFixture(@PathParam("seasonId") @Positive long seasonId) {
        return fixtureService.getFixture(seasonId);
    }

    /**
     * Takes no body. The new fixture's address is this same path.
     */
    @POST
    public Response generateFixture(@PathParam("seasonId") @Positive long seasonId, @Context UriInfo uriInfo) {
        final FixtureResponse created = fixtureService.generateFixture(seasonId);
        return Response.created(uriInfo.getAbsolutePath()).entity(created).build();
    }
}

package com.basketballleague.resource;

import com.basketballleague.dto.PingResponse;
import com.basketballleague.service.HealthService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Liveness endpoint: {@code GET /api/ping}.
 */
@Path("ping")
@RequestScoped
public class PingResource {

    private HealthService healthService;

    /** Required by CDI to create the client proxy. */
    protected PingResource() {
    }

    @Inject
    public PingResource(HealthService healthService) {
        this.healthService = healthService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public PingResponse ping() {
        return healthService.ping();
    }
}

package com.basketballleague.service;

import com.basketballleague.dto.PingResponse;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Clock;
import java.time.Instant;

/**
 * Reports the application's health status.
 */
@ApplicationScoped
public class HealthService {

    static final String STATUS_UP = "UP";

    private final Clock clock;

    public HealthService() {
        this(Clock.systemUTC());
    }

    HealthService(Clock clock) {
        this.clock = clock;
    }

    public PingResponse ping() {
        return new PingResponse(STATUS_UP, Instant.now(clock));
    }
}

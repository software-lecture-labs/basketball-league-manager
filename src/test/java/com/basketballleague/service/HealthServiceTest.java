package com.basketballleague.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.basketballleague.dto.PingResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class HealthServiceTest {

    @Test
    void pingReportsUpWithCurrentTime() {
        Instant now = Instant.parse("2026-01-01T12:00:00Z");
        HealthService healthService = new HealthService(Clock.fixed(now, ZoneOffset.UTC));

        PingResponse response = healthService.ping();

        assertThat(response.status()).isEqualTo("UP");
        assertThat(response.timestamp()).isEqualTo(now);
    }
}

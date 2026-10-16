package com.basketballleague.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.basketballleague.dto.PingResponse;
import com.basketballleague.service.HealthService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PingResourceTest {

    @Mock
    private HealthService healthService;

    @InjectMocks
    private PingResource pingResource;

    @Test
    void pingReturnsResponseFromService() {
        PingResponse expected = new PingResponse("UP", Instant.parse("2026-01-01T12:00:00Z"));
        when(healthService.ping()).thenReturn(expected);

        PingResponse actual = pingResource.ping();

        assertThat(actual).isSameAs(expected);
        verify(healthService).ping();
    }
}

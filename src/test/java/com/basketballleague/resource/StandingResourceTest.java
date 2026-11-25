package com.basketballleague.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.basketballleague.dto.StandingResponse;
import com.basketballleague.service.StandingService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StandingResourceTest {

    @Mock
    private StandingService standingService;

    @Test
    void getStandingsReturnsTheTableFromTheService() {
        final List<StandingResponse> table = List.of(new StandingResponse(1, 4L, "Anadolu Efes", 0, 0, 0, 0, 0, 0, 0));
        when(standingService.calculateStandings(1L)).thenReturn(table);

        assertThat(new StandingResource(standingService).getStandings(1L)).isSameAs(table);
    }

    @Test
    void rejectsANonPositiveSeasonId() throws NoSuchMethodException {
        final Method getStandings = StandingResource.class.getMethod("getStandings", long.class);

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            final Validator validator = factory.getValidator();
            final StandingResource target = new StandingResource(null);
            assertThat(validator.forExecutables().validateParameters(target, getStandings, new Object[] {0L}))
                    .hasSize(1);
            assertThat(validator.forExecutables().validateParameters(target, getStandings, new Object[] {1L}))
                    .isEmpty();
        }
    }
}

package com.basketballleague.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.basketballleague.dto.FixtureResponse;
import com.basketballleague.service.FixtureService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FixtureResourceTest {

    private static final FixtureResponse FIXTURE = new FixtureResponse(1L, List.of());
    private static final URI FIXTURE_URI = URI.create("http://localhost:8080/basketball-league/api/seasons/1/fixtures");

    @Mock
    private FixtureService fixtureService;

    @Mock
    private UriInfo uriInfo;

    private FixtureResource resource() {
        return new FixtureResource(fixtureService);
    }

    @Test
    void getFixtureReturnsTheFixtureFromTheService() {
        when(fixtureService.getFixture(1L)).thenReturn(FIXTURE);

        assertThat(resource().getFixture(1L)).isSameAs(FIXTURE);
    }

    @Test
    void generateFixtureAnswers201WithTheFixturesAddress() {
        when(fixtureService.generateFixture(1L)).thenReturn(FIXTURE);
        when(uriInfo.getAbsolutePath()).thenReturn(FIXTURE_URI);

        final Response response = resource().generateFixture(1L, uriInfo);

        assertThat(response.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
        assertThat(response.getLocation()).isEqualTo(FIXTURE_URI);
        assertThat(response.getEntity()).isEqualTo(FIXTURE);
    }

    @Test
    void rejectsANonPositiveSeasonId() throws NoSuchMethodException {
        final Method getFixture = FixtureResource.class.getMethod("getFixture", long.class);
        final Method generate = FixtureResource.class.getMethod("generateFixture", long.class, UriInfo.class);

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            final Validator validator = factory.getValidator();
            final FixtureResource target = new FixtureResource(null);
            assertThat(validator.forExecutables().validateParameters(target, getFixture, new Object[] {0L}))
                    .hasSize(1);
            assertThat(validator.forExecutables().validateParameters(target, generate, new Object[] {-1L, null}))
                    .hasSize(1);
            assertThat(validator.forExecutables().validateParameters(target, getFixture, new Object[] {1L}))
                    .isEmpty();
        }
    }
}

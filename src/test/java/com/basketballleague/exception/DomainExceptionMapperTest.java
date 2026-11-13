package com.basketballleague.exception;

import static com.basketballleague.exception.ProblemResponseAssertions.assertProblem;
import static org.assertj.core.api.Assertions.assertThat;

import com.basketballleague.domain.InvalidFixtureInputException;
import com.basketballleague.dto.ProblemDetails;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

class DomainExceptionMapperTest {

    private final DomainExceptionMapper mapper = new DomainExceptionMapper();

    @Test
    void mapsBusinessRuleViolationToConflictWithItsCode() {
        final InvalidFixtureInputException exception = new InvalidFixtureInputException("Two teams are required.");

        final Response response = mapper.toResponse(exception);

        final ProblemDetails body = assertProblem(response, Response.Status.CONFLICT, "INVALID_FIXTURE_INPUT");
        assertThat(body.detail()).isEqualTo("Fikstür için birbirinden farklı en az iki takım gerekir.");
        assertThat(body.errors()).isNull();
    }

    @Test
    void neverSendsTheExceptionMessageToTheClient() {
        final String serverOnlyMessage = "Team identifiers must be non-null and unique.";

        final Response response = mapper.toResponse(new InvalidFixtureInputException(serverOnlyMessage));

        assertThat(((ProblemDetails) response.getEntity()).detail()).doesNotContain(serverOnlyMessage);
    }
}

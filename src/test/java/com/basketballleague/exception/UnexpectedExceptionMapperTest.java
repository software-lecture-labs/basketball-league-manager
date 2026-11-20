package com.basketballleague.exception;

import static com.basketballleague.exception.ProblemResponseAssertions.assertProblem;
import static org.assertj.core.api.Assertions.assertThat;

import com.basketballleague.dto.ProblemDetails;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

class UnexpectedExceptionMapperTest {

    private final UnexpectedExceptionMapper mapper = new UnexpectedExceptionMapper();

    @Test
    void returnsInternalErrorWithoutDetailsOfTheFailure() {
        final IllegalStateException failure =
                new IllegalStateException("duplicate key value violates unique constraint \"uq_team_name\"");

        final Response response = mapper.toResponse(failure);

        final ProblemDetails body = assertProblem(response, Response.Status.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR");
        assertThat(body.detail()).doesNotContain("uq_team_name").doesNotContain("duplicate");
        assertThat(body.errors()).isNull();
    }
}

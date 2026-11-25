package com.basketballleague.exception;

import static com.basketballleague.exception.ProblemResponseAssertions.assertProblem;
import static org.assertj.core.api.Assertions.assertThat;

import com.basketballleague.dto.ProblemDetails;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.core.Response;
import java.sql.SQLException;
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

    @Test
    void answersAConcurrentUpdateWith409() {
        final Response response = mapper.toResponse(
                new RuntimeException("commit failed", new OptimisticLockException("Game 7 was changed")));

        final ProblemDetails body = assertProblem(response, Response.Status.CONFLICT, "CONCURRENT_UPDATE");
        assertThat(body.detail()).doesNotContain("Game 7");
    }

    @Test
    void answersAnIntegrityViolationWith409WithoutConstraintDetails() {
        final Response response = mapper.toResponse(new PersistenceException("flush failed",
                new SQLException("duplicate key value violates unique constraint \"uq_seasons_name\"", "23505")));

        final ProblemDetails body = assertProblem(response, Response.Status.CONFLICT, "DATA_CONFLICT");
        assertThat(body.detail()).doesNotContain("uq_seasons_name").doesNotContain("duplicate");
        assertThat(body.errors()).isNull();
    }
}

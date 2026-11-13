package com.basketballleague.exception;

import static com.basketballleague.exception.ProblemResponseAssertions.assertProblem;
import static org.assertj.core.api.Assertions.assertThat;

import com.basketballleague.domain.ErrorCode;
import com.basketballleague.domain.RecordNotFoundException;
import com.basketballleague.dto.ProblemDetails;
import jakarta.ws.rs.core.Response;
import java.io.Serial;
import org.junit.jupiter.api.Test;

class RecordNotFoundExceptionMapperTest {

    private final RecordNotFoundExceptionMapper mapper = new RecordNotFoundExceptionMapper();

    @Test
    void mapsMissingRecordToNotFoundWithoutTheServerMessage() {
        final Response response = mapper.toResponse(new MissingRecordException("Team 42 does not exist"));

        final ProblemDetails body = assertProblem(response, Response.Status.NOT_FOUND, "INVALID_FIXTURE_INPUT");
        assertThat(body.detail()).doesNotContain("42");
    }

    /**
     * No not-found code exists yet; the mapper's status depends only on the exception type.
     */
    private static final class MissingRecordException extends RecordNotFoundException {

        @Serial
        private static final long serialVersionUID = 1L;

        MissingRecordException(String message) {
            super(ErrorCode.INVALID_FIXTURE_INPUT, message);
        }
    }
}

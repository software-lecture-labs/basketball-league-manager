package com.basketballleague.exception;

import static com.basketballleague.exception.ProblemResponseAssertions.assertProblem;

import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

class ProcessingExceptionMapperTest {

    private final ProcessingExceptionMapper mapper = new ProcessingExceptionMapper();

    @Test
    void reportsUnreadableJsonAsMalformedRequest() {
        // Same shape as Jersey's JSON-B reader in Payara: ProcessingException caused by JsonbException.
        final ProcessingException exception = new ProcessingException("Error deserializing object",
                new JsonbException("Unexpected char"));

        assertProblem(mapper.toResponse(exception), Response.Status.BAD_REQUEST, "MALFORMED_REQUEST");
    }

    @Test
    void findsJsonbExceptionDeeperInTheCauseChain() {
        final ProcessingException exception =
                new ProcessingException(new IllegalStateException(new JsonbException("Unexpected char")));

        assertProblem(mapper.toResponse(exception), Response.Status.BAD_REQUEST, "MALFORMED_REQUEST");
    }

    @Test
    void treatsOtherProcessingFailuresAsUnexpected() {
        final ProcessingException exception = new ProcessingException("Writer failed");

        assertProblem(mapper.toResponse(exception), Response.Status.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR");
    }

    @Test
    void stopsOnACauseChainThatPointsBackToItself() {
        final IllegalStateException first = new IllegalStateException("first");
        final IllegalStateException second = new IllegalStateException("second", first);
        first.initCause(second);

        final Response response = mapper.toResponse(new ProcessingException(first));

        assertProblem(response, Response.Status.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR");
    }
}

package com.basketballleague.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Every failure without a more specific mapper. A concurrent update or a data integrity violation in the cause
 * chain becomes 409; anything else is 500 without any detail of the failure, and its stack trace goes to the
 * server log only (ADR-0006).
 */
@Provider
public class UnexpectedExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnexpectedExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        final Optional<GeneralProblem> conflict = PersistenceConflicts.classify(exception);
        if (conflict.isEmpty()) {
            LOGGER.error("Unexpected error while processing a request", exception);
            return GeneralProblem.INTERNAL_ERROR.toResponse();
        }
        if (conflict.get() == GeneralProblem.CONCURRENT_UPDATE) {
            LOGGER.info("Concurrent update rejected: {}", exception.toString());
        } else {
            LOGGER.warn("Data integrity violation rejected: {}", exception.toString());
        }
        return conflict.get().toResponse();
    }
}

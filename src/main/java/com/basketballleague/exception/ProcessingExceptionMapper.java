package com.basketballleague.exception;

import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jersey reports an unreadable JSON body as a {@link ProcessingException} caused by a {@link JsonbException};
 * that case is a client error (400). Any other processing failure is unexpected (500).
 */
@Provider
public class ProcessingExceptionMapper implements ExceptionMapper<ProcessingException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingExceptionMapper.class);

    @Override
    public Response toResponse(ProcessingException exception) {
        if (isCausedByJsonb(exception)) {
            LOGGER.debug("Request body could not be read: {}", exception.getMessage());
            return GeneralProblem.MALFORMED_REQUEST.toResponse();
        }
        LOGGER.error("Request processing failed", exception);
        return GeneralProblem.INTERNAL_ERROR.toResponse();
    }

    private static boolean isCausedByJsonb(Throwable exception) {
        // The visited set stops a cause chain that points back to itself.
        final Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable current = exception; current != null && visited.add(current); current = current.getCause()) {
            if (current instanceof JsonbException) {
                return true;
            }
        }
        return false;
    }
}

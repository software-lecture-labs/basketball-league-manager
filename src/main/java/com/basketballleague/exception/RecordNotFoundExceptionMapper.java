package com.basketballleague.exception;

import com.basketballleague.domain.RecordNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Requested record does not exist: 404. Jakarta REST picks the mapper of the nearest superclass, so this mapper
 * wins over {@link DomainExceptionMapper} for not found errors.
 */
@Provider
public class RecordNotFoundExceptionMapper implements ExceptionMapper<RecordNotFoundException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecordNotFoundExceptionMapper.class);

    @Override
    public Response toResponse(RecordNotFoundException exception) {
        LOGGER.debug("Record not found ({}): {}", exception.code(), exception.getMessage());
        return ProblemResponses.of(Response.Status.NOT_FOUND, exception.code());
    }
}

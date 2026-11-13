package com.basketballleague.exception;

import com.basketballleague.domain.DomainException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Business rule violation: 409. Subtypes with their own mapper (such as not found) do not reach this class.
 */
@Provider
public class DomainExceptionMapper implements ExceptionMapper<DomainException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DomainExceptionMapper.class);

    @Override
    public Response toResponse(DomainException exception) {
        LOGGER.info("Business rule violated ({}): {}", exception.code(), exception.getMessage());
        return ProblemResponses.of(Response.Status.CONFLICT, exception.code());
    }
}

package com.basketballleague.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Last resort: 500 without any detail of the failure. The stack trace is written to the server log only.
 */
@Provider
public class UnexpectedExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnexpectedExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        LOGGER.error("Unexpected error while processing a request", exception);
        return GeneralProblem.INTERNAL_ERROR.toResponse();
    }
}

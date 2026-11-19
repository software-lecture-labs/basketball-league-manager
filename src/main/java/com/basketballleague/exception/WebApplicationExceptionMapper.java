package com.basketballleague.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Errors raised by Jakarta REST itself (unknown path, unsupported method or media type, empty body).
 * Without this mapper {@link UnexpectedExceptionMapper} would turn them into 500.
 */
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebApplicationExceptionMapper.class);
    private static final String HTTP_CODE_PREFIX = "HTTP_";

    @Override
    public Response toResponse(WebApplicationException exception) {
        final Response original = exception.getResponse();
        final Response.StatusType status = original.getStatusInfo();
        switch (status.getFamily()) {
            case CLIENT_ERROR -> {
                LOGGER.debug("Request rejected with {}: {}", status.getStatusCode(), exception.getMessage());
                if (status.getStatusCode() == Response.Status.BAD_REQUEST.getStatusCode()) {
                    return GeneralProblem.MALFORMED_REQUEST.toResponse();
                }
                final Response problem = ProblemResponses.of(status, HTTP_CODE_PREFIX + status.getStatusCode(),
                        ProblemCatalog.detailForHttpStatus(status.getStatusCode()), null);
                return withHeadersOf(original, problem);
            }
            case SERVER_ERROR -> {
                LOGGER.error("Request failed with {}", status.getStatusCode(), exception);
                return GeneralProblem.INTERNAL_ERROR.toResponse();
            }
            default -> {
                // Redirects and other non-error responses keep their headers (e.g. Location) unchanged.
                return original;
            }
        }
    }

    /**
     * HTTP requires some error responses to carry headers (e.g. {@code Allow} on 405), so the original headers
     * are copied. Content-Type and Content-Length described the original body and are left out.
     */
    private static Response withHeadersOf(Response original, Response problem) {
        final Response.ResponseBuilder builder = Response.fromResponse(problem);
        original.getHeaders().forEach((name, values) -> {
            if (!describesBody(name)) {
                values.forEach(value -> builder.header(name, value));
            }
        });
        return builder.build();
    }

    private static boolean describesBody(String headerName) {
        return HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(headerName)
                || HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(headerName);
    }
}

package com.basketballleague.exception;

import com.basketballleague.domain.ErrorCode;
import com.basketballleague.dto.FieldViolation;
import com.basketballleague.dto.ProblemDetails;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * Builds every error response, so the body format of ADR-0006 is defined in one place.
 */
final class ProblemResponses {

    static final MediaType PROBLEM_JSON = new MediaType("application", "problem+json");

    private static final String BLANK_TYPE = "about:blank";

    private ProblemResponses() {
    }

    static Response of(Response.StatusType status, ErrorCode code) {
        return of(status, code.name(), ProblemCatalog.detailFor(code), null);
    }

    static Response of(Response.StatusType status, String code, String detail, List<FieldViolation> errors) {
        final ProblemDetails body = new ProblemDetails(
                BLANK_TYPE, status.getReasonPhrase(), status.getStatusCode(), detail, code, errors);
        return Response.status(status).type(PROBLEM_JSON).entity(body).build();
    }
}

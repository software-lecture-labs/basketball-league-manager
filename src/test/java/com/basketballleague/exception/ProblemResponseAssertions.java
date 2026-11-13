package com.basketballleague.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.basketballleague.dto.ProblemDetails;
import jakarta.ws.rs.core.Response;

/**
 * Checks the parts of ADR-0006 that every error response shares.
 */
final class ProblemResponseAssertions {

    private ProblemResponseAssertions() {
    }

    static ProblemDetails assertProblem(Response response, Response.Status status, String code) {
        assertThat(response.getStatus()).isEqualTo(status.getStatusCode());
        assertThat(response.getMediaType()).hasToString("application/problem+json");
        assertThat(response.getEntity()).isInstanceOf(ProblemDetails.class);
        final ProblemDetails body = (ProblemDetails) response.getEntity();
        assertThat(body.type()).isEqualTo("about:blank");
        assertThat(body.title()).isEqualTo(status.getReasonPhrase());
        assertThat(body.status()).isEqualTo(status.getStatusCode());
        assertThat(body.code()).isEqualTo(code);
        assertThat(body.detail()).isNotBlank();
        return body;
    }
}

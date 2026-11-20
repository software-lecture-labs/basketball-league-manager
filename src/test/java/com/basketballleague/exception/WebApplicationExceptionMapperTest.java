package com.basketballleague.exception;

import static com.basketballleague.exception.ProblemResponseAssertions.assertProblem;
import static org.assertj.core.api.Assertions.assertThat;

import com.basketballleague.dto.ProblemDetails;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.RedirectionException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import org.junit.jupiter.api.Test;

class WebApplicationExceptionMapperTest {

    private final WebApplicationExceptionMapper mapper = new WebApplicationExceptionMapper();

    @Test
    void keepsNotFoundForAnUnknownPath() {
        final Response response = mapper.toResponse(new NotFoundException());

        final ProblemDetails body = assertProblem(response, Response.Status.NOT_FOUND, "HTTP_404");
        assertThat(body.detail()).isEqualTo("İstenen adres bulunamadı.");
    }

    @Test
    void keepsMethodNotAllowedWithTheAllowHeader() {
        final Response response = mapper.toResponse(new NotAllowedException("GET"));

        assertProblem(response, Response.Status.METHOD_NOT_ALLOWED, "HTTP_405");
        assertThat(response.getHeaderString(HttpHeaders.ALLOW)).isEqualTo("GET");
    }

    @Test
    void keepsTheOriginalHeadersButNotTheOriginalBodyHeaders() {
        final Response original = Response.status(Response.Status.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, "120")
                .type(MediaType.TEXT_PLAIN_TYPE)
                .header(HttpHeaders.CONTENT_LENGTH, "42")
                .entity("slow down")
                .build();

        final Response response = mapper.toResponse(new ClientErrorException(original));

        assertProblem(response, Response.Status.TOO_MANY_REQUESTS, "HTTP_429");
        assertThat(response.getHeaderString(HttpHeaders.RETRY_AFTER)).isEqualTo("120");
        assertThat(response.getHeaderString(HttpHeaders.CONTENT_LENGTH)).isNull();
    }

    @Test
    void keepsUnsupportedMediaType() {
        final Response response = mapper.toResponse(new NotSupportedException());

        assertProblem(response, Response.Status.UNSUPPORTED_MEDIA_TYPE, "HTTP_415");
    }

    @Test
    void usesAGeneralTextForOtherClientErrors() {
        final Response response = mapper.toResponse(new ClientErrorException(Response.Status.CONFLICT));

        final ProblemDetails body = assertProblem(response, Response.Status.CONFLICT, "HTTP_409");
        assertThat(body.detail()).isEqualTo("İstek işlenemedi.");
    }

    @Test
    void reportsAnUnreadableRequestAsMalformed() {
        final Response response = mapper.toResponse(new BadRequestException());

        assertProblem(response, Response.Status.BAD_REQUEST, "MALFORMED_REQUEST");
    }

    @Test
    void hidesServerErrorsBehindTheGeneralInternalError() {
        final Response response = mapper.toResponse(new InternalServerErrorException("database is down"));

        final ProblemDetails body = assertProblem(response, Response.Status.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR");
        assertThat(body.detail()).doesNotContain("database");
    }

    @Test
    void returnsRedirectsUnchanged() {
        final RedirectionException redirect =
                new RedirectionException(Response.Status.SEE_OTHER, URI.create("/basketball-league/api/teams/1"));

        final Response response = mapper.toResponse(redirect);

        assertThat(response).isSameAs(redirect.getResponse());
    }
}

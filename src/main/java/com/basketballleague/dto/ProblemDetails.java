package com.basketballleague.dto;

import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.util.List;

/**
 * RFC 9457 error body, sent as {@code application/problem+json}. {@code errors} is set only for validation
 * failures; when it is null JSON-B leaves it out of the response.
 */
@JsonbPropertyOrder({"type", "title", "status", "detail", "code", "errors"})
public record ProblemDetails(String type, String title, int status, String detail, String code,
        List<FieldViolation> errors) {

    public ProblemDetails {
        errors = errors == null ? null : List.copyOf(errors);
    }
}

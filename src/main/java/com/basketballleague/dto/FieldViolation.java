package com.basketballleague.dto;

/**
 * One invalid value in a request. {@code field} is the path inside the request body (for example
 * {@code players[0].jerseyNumber}); it is null when the constraint is not on a body field.
 */
public record FieldViolation(String field, String message) {
}

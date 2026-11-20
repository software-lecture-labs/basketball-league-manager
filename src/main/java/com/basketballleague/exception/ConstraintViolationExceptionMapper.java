package com.basketballleague.exception;

import com.basketballleague.dto.FieldViolation;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bean Validation failure: 400 with the list of invalid fields. A violation on a return value is a server bug
 * and becomes 500, as Jakarta REST requires.
 */
@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConstraintViolationExceptionMapper.class);

    private static final Comparator<FieldViolation> BY_FIELD_THEN_MESSAGE = Comparator
            .comparing(FieldViolation::field, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(FieldViolation::message, Comparator.nullsFirst(Comparator.naturalOrder()));

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        final Set<ConstraintViolation<?>> violations =
                Objects.requireNonNullElse(exception.getConstraintViolations(), Set.of());
        if (violations.stream().anyMatch(ConstraintViolationExceptionMapper::isOnReturnValue)) {
            LOGGER.error("Response validation failed", exception);
            return GeneralProblem.INTERNAL_ERROR.toResponse();
        }
        LOGGER.debug("Request validation failed: {}", exception.getMessage());
        final List<FieldViolation> errors = violations.stream()
                .map(violation -> new FieldViolation(bodyFieldOf(violation.getPropertyPath()), violation.getMessage()))
                .sorted(BY_FIELD_THEN_MESSAGE)
                .toList();
        return GeneralProblem.VALIDATION_FAILED.toResponse(errors);
    }

    private static boolean isOnReturnValue(ConstraintViolation<?> violation) {
        for (Path.Node node : violation.getPropertyPath()) {
            if (node.getKind() == ElementKind.RETURN_VALUE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Keeps only the part of the path inside the request body; Java method and parameter names are dropped.
     * {@code create.arg0.players[0].jerseyNumber} becomes {@code players[0].jerseyNumber}.
     */
    private static String bodyFieldOf(Path path) {
        final StringBuilder field = new StringBuilder();
        for (Path.Node node : path) {
            final ElementKind kind = node.getKind();
            if (kind != ElementKind.PROPERTY && kind != ElementKind.CONTAINER_ELEMENT) {
                continue;
            }
            // The index of a list element is stored on the node that follows the list.
            if (node.isInIterable()) {
                final Object position = node.getIndex() != null ? node.getIndex() : node.getKey();
                field.append('[').append(position).append(']');
            }
            if (kind == ElementKind.PROPERTY) {
                if (!field.isEmpty()) {
                    field.append('.');
                }
                field.append(node.getName());
            }
        }
        return field.isEmpty() ? null : field.toString();
    }
}

package com.basketballleague.exception;

import static com.basketballleague.exception.ProblemResponseAssertions.assertProblem;
import static org.assertj.core.api.Assertions.assertThat;

import com.basketballleague.dto.FieldViolation;
import com.basketballleague.dto.ProblemDetails;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.executable.ExecutableValidator;
import jakarta.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Uses a real Bean Validation implementation so the violation paths look like the ones Payara produces.
 */
class ConstraintViolationExceptionMapperTest {

    private static ValidatorFactory validatorFactory;
    private static ExecutableValidator validator;

    private final ConstraintViolationExceptionMapper mapper = new ConstraintViolationExceptionMapper();
    private final TeamEndpoint endpoint = new TeamEndpoint();

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator().forExecutables();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void listsInvalidBodyFieldsWithoutJavaMethodOrParameterNames() throws NoSuchMethodException {
        final TeamRequest request = new TeamRequest(" ", List.of(new PlayerRequest(7), new PlayerRequest(100)));

        final Response response = mapper.toResponse(violationsOf("create", TeamRequest.class, request));

        final ProblemDetails body = assertProblem(response, Response.Status.BAD_REQUEST, "VALIDATION_FAILED");
        assertThat(body.errors()).containsExactly(
                new FieldViolation("name", "boş bırakılamaz"),
                new FieldViolation("players[1].jerseyNumber", "99 veya daha küçük olmalıdır"));
    }

    @Test
    void leavesFieldEmptyWhenTheConstraintIsOnAQueryParameter() throws NoSuchMethodException {
        final Response response = mapper.toResponse(violationsOf("list", int.class, 101));

        final ProblemDetails body = assertProblem(response, Response.Status.BAD_REQUEST, "VALIDATION_FAILED");
        assertThat(body.errors()).containsExactly(new FieldViolation(null, "100 veya daha küçük olmalıdır"));
    }

    @Test
    void treatsAnInvalidReturnValueAsServerError() throws NoSuchMethodException {
        final Method method = TeamEndpoint.class.getDeclaredMethod("name");
        final Set<ConstraintViolation<TeamEndpoint>> violations = validator.validateReturnValue(endpoint, method, null);

        final Response response = mapper.toResponse(new ConstraintViolationException(new HashSet<>(violations)));

        final ProblemDetails body = assertProblem(response, Response.Status.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR");
        assertThat(body.errors()).isNull();
    }

    @Test
    void acceptsAnExceptionWithoutViolations() {
        final Response response = mapper.toResponse(new ConstraintViolationException("no details", null));

        final ProblemDetails body = assertProblem(response, Response.Status.BAD_REQUEST, "VALIDATION_FAILED");
        assertThat(body.errors()).isEmpty();
    }

    private ConstraintViolationException violationsOf(String methodName, Class<?> parameterType, Object argument)
            throws NoSuchMethodException {
        final Method method = TeamEndpoint.class.getDeclaredMethod(methodName, parameterType);
        final Set<ConstraintViolation<TeamEndpoint>> violations =
                validator.validateParameters(endpoint, method, new Object[] {argument});
        assertThat(violations).isNotEmpty();
        return new ConstraintViolationException(new HashSet<>(violations));
    }

    record TeamRequest(@NotBlank String name, @Valid List<PlayerRequest> players) {
    }

    record PlayerRequest(@Min(0) @Max(99) int jerseyNumber) {
    }

    /**
     * Stands in for a resource class; only the constraint annotations on the signatures matter.
     */
    static class TeamEndpoint {

        TeamRequest create(@Valid TeamRequest request) {
            return request;
        }

        int list(@Max(100) int size) {
            return size;
        }

        @NotNull
        String name() {
            return null;
        }
    }
}

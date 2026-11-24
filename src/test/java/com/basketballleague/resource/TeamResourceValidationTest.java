package com.basketballleague.resource;

import static org.assertj.core.api.Assertions.assertThat;

import com.basketballleague.dto.CreatePlayerRequest;
import com.basketballleague.dto.CreateTeamRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Checks the request rules of docs/api.md with a real Bean Validation implementation, the way Payara applies them
 * before a resource method runs. Violations become 400 in ConstraintViolationExceptionMapper.
 */
class TeamResourceValidationTest {

    private static final String LONGEST_NAME = "a".repeat(100);
    private static final String TOO_LONG_NAME = "a".repeat(101);

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    private final TeamResource resource = new TeamResource(null);

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void acceptsAValidPlayer() {
        assertThat(validator.validate(new CreatePlayerRequest("Şükrü", "Işık", 0, "C"))).isEmpty();
        assertThat(validator.validate(new CreatePlayerRequest(LONGEST_NAME, LONGEST_NAME, 99, "PG"))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 100, Integer.MAX_VALUE, Integer.MIN_VALUE})
    void rejectsJerseyNumbersOutsideZeroToNinetyNine(int jerseyNumber) {
        assertThat(fieldsOf(validator.validate(new CreatePlayerRequest("Shane", "Larkin", jerseyNumber, "PG"))))
                .containsExactly("jerseyNumber");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "G", "pg", "PG ", "CF"})
    void rejectsUnknownPositions(String position) {
        assertThat(fieldsOf(validator.validate(new CreatePlayerRequest("Shane", "Larkin", 3, position))))
                .containsExactly("position");
    }

    @Test
    void rejectsMissingOrTooLongPlayerFields() {
        assertThat(fieldsOf(validator.validate(new CreatePlayerRequest(" ", TOO_LONG_NAME, null, "PG"))))
                .containsExactlyInAnyOrder("firstName", "lastName", "jerseyNumber");
    }

    @Test
    void rejectsMissingOrTooLongTeamFields() {
        assertThat(validator.validate(new CreateTeamRequest(LONGEST_NAME, "İstanbul"))).isEmpty();
        assertThat(fieldsOf(validator.validate(new CreateTeamRequest(TOO_LONG_NAME, "  "))))
                .containsExactlyInAnyOrder("name", "city");
    }

    @ParameterizedTest
    @CsvSource({"0, 1", "0, 100", "21474836, 100"})
    void acceptsPageAndSizeOnTheBoundaries(int page, int size) throws NoSuchMethodException {
        assertThat(listTeamsViolations(page, size)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"-1, 20", "0, 0", "0, 101", "21474837, 100"})
    void rejectsPageOrSizeOutsideTheLimits(int page, int size) throws NoSuchMethodException {
        assertThat(listTeamsViolations(page, size)).hasSize(1);
    }

    @Test
    void rejectsAMissingBody() throws NoSuchMethodException {
        final Method createTeam = TeamResource.class.getMethod("createTeam", CreateTeamRequest.class, UriInfo.class);

        assertThat(validator.forExecutables().validateParameters(resource, createTeam, new Object[] {null, null}))
                .hasSize(1);
    }

    private Set<ConstraintViolation<TeamResource>> listTeamsViolations(int page, int size)
            throws NoSuchMethodException {
        final Method listTeams = TeamResource.class.getMethod("listTeams", int.class, int.class);
        return validator.forExecutables().validateParameters(resource, listTeams, new Object[] {page, size});
    }

    private static <T> Set<String> fieldsOf(Set<ConstraintViolation<T>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}

package com.basketballleague.resource;

import static org.assertj.core.api.Assertions.assertThat;

import com.basketballleague.dto.AddSeasonTeamRequest;
import com.basketballleague.dto.CreateSeasonRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Checks the request rules of docs/api.md for seasons with a real Bean Validation implementation, the way Payara
 * applies them before a resource method runs. Violations become 400 in ConstraintViolationExceptionMapper.
 */
class SeasonResourceValidationTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 1);

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    private final SeasonResource resource = new SeasonResource(null);

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
    void acceptsAValidSeason() {
        assertThat(validator.validate(new CreateSeasonRequest("2026-27 Şampiyonası", START))).isEmpty();
        assertThat(validator.validate(new CreateSeasonRequest("a".repeat(100), START))).isEmpty();
    }

    @Test
    void rejectsMissingOrTooLongSeasonFields() {
        assertThat(fieldsOf(validator.validate(new CreateSeasonRequest("a".repeat(101), null))))
                .containsExactlyInAnyOrder("name", "startDate");
        assertThat(fieldsOf(validator.validate(new CreateSeasonRequest("  ", START))))
                .containsExactly("name");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L, Long.MIN_VALUE})
    void rejectsAMissingOrNonPositiveTeamId(Long teamId) {
        assertThat(fieldsOf(validator.validate(new AddSeasonTeamRequest(teamId)))).containsExactly("teamId");
    }

    @Test
    void acceptsAPositiveTeamId() {
        assertThat(validator.validate(new AddSeasonTeamRequest(1L))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void rejectsANonPositiveSeasonId(long seasonId) throws NoSuchMethodException {
        final Method getSeason = SeasonResource.class.getMethod("getSeason", long.class);

        assertThat(validator.forExecutables().validateParameters(resource, getSeason, new Object[] {seasonId}))
                .hasSize(1);
    }

    @Test
    void rejectsAMissingBody() throws NoSuchMethodException {
        final Method createSeason =
                SeasonResource.class.getMethod("createSeason", CreateSeasonRequest.class, UriInfo.class);
        final Method addTeam = SeasonResource.class.getMethod("addTeam", long.class, AddSeasonTeamRequest.class);

        assertThat(validator.forExecutables().validateParameters(resource, createSeason, new Object[] {null, null}))
                .hasSize(1);
        assertThat(validator.forExecutables().validateParameters(resource, addTeam, new Object[] {1L, null}))
                .hasSize(1);
    }

    @Test
    void rejectsPageOrSizeOutsideTheLimits() throws NoSuchMethodException {
        final Method listSeasons = SeasonResource.class.getMethod("listSeasons", int.class, int.class);

        assertThat(validator.forExecutables().validateParameters(resource, listSeasons, new Object[] {0, 20}))
                .isEmpty();
        assertThat(validator.forExecutables().validateParameters(resource, listSeasons, new Object[] {-1, 101}))
                .hasSize(2);
    }

    private static <T> Set<String> fieldsOf(Set<ConstraintViolation<T>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}

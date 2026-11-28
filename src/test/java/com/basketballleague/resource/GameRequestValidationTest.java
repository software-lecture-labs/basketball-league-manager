package com.basketballleague.resource;

import static org.assertj.core.api.Assertions.assertThat;

import com.basketballleague.dto.GameVersionRequest;
import com.basketballleague.dto.PlayerPointsRequest;
import com.basketballleague.dto.QuarterScoreRequest;
import com.basketballleague.dto.RecordPlayerPointsRequest;
import com.basketballleague.dto.RecordQuartersRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Checks the request rules of docs/api.md for game result entry with a real Bean Validation implementation.
 * The upper limits keep totals far from int overflow, so Math.addExact in Game never fails with a 500.
 */
class GameRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

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
    void acceptsValidRequests() {
        assertThat(validator.validate(new GameVersionRequest(0L))).isEmpty();
        assertThat(validator.validate(new RecordQuartersRequest(3L, List.of(
                new QuarterScoreRequest(1, 22, 18), new QuarterScoreRequest(2, 100, 0))))).isEmpty();
        assertThat(validator.validate(new RecordPlayerPointsRequest(5L, List.of(
                new PlayerPointsRequest(7L, 21), new PlayerPointsRequest(40L, 200))))).isEmpty();
        assertThat(validator.validate(new RecordPlayerPointsRequest(5L, List.of()))).isEmpty();
    }

    @Test
    void rejectsMissingOrNegativeVersion() {
        assertThat(fieldsOf(validator.validate(new GameVersionRequest(null)))).containsExactly("version");
        assertThat(fieldsOf(validator.validate(new GameVersionRequest(-1L)))).containsExactly("version");
        assertThat(fieldsOf(validator.validate(new RecordQuartersRequest(-1L, List.of()))))
                .containsExactly("version");
        assertThat(fieldsOf(validator.validate(new RecordPlayerPointsRequest(null, List.of()))))
                .containsExactly("version");
    }

    @Test
    void rejectsPointsAboveLimitsOrBelowZero() {
        assertThat(fieldsOf(validator.validate(new RecordQuartersRequest(1L, List.of(
                new QuarterScoreRequest(1, 101, -1))))))
                .containsExactlyInAnyOrder("quarters[0].homePoints", "quarters[0].awayPoints");
        assertThat(fieldsOf(validator.validate(new RecordPlayerPointsRequest(1L, List.of(
                new PlayerPointsRequest(7L, 201))))))
                .containsExactly("players[0].points");
    }

    @Test
    void rejectsMissingFieldsInListElements() {
        assertThat(fieldsOf(validator.validate(new RecordQuartersRequest(1L, List.of(
                new QuarterScoreRequest(0, null, 10))))))
                .containsExactlyInAnyOrder("quarters[0].quarterNo", "quarters[0].homePoints");
        assertThat(fieldsOf(validator.validate(new RecordPlayerPointsRequest(1L, List.of(
                new PlayerPointsRequest(0L, null))))))
                .containsExactlyInAnyOrder("players[0].playerId", "players[0].points");
    }

    @Test
    void rejectsMissingOrNullListElements() {
        final List<QuarterScoreRequest> withNull = new ArrayList<>(Collections.singletonList(null));

        assertThat(fieldsOf(validator.validate(new RecordQuartersRequest(1L, null)))).containsExactly("quarters");
        assertThat(fieldsOf(validator.validate(new RecordQuartersRequest(1L, withNull))))
                .containsExactly("quarters[0].<list element>");
        assertThat(fieldsOf(validator.validate(new RecordPlayerPointsRequest(1L, null))))
                .containsExactly("players");
    }

    @Test
    void rejectsTooManyQuartersOrPlayers() {
        final List<QuarterScoreRequest> elevenQuarters = IntStream.rangeClosed(1, 11)
                .mapToObj(number -> new QuarterScoreRequest(number, 10, 10)).toList();
        final List<PlayerPointsRequest> fortyOnePlayers = LongStream.rangeClosed(1, 41)
                .mapToObj(id -> new PlayerPointsRequest(id, 1)).toList();

        assertThat(fieldsOf(validator.validate(new RecordQuartersRequest(1L, elevenQuarters))))
                .containsExactly("quarters");
        assertThat(fieldsOf(validator.validate(new RecordPlayerPointsRequest(1L, fortyOnePlayers))))
                .containsExactly("players");
    }

    @Test
    void rejectsTheSamePlayerTwice() {
        assertThat(fieldsOf(validator.validate(new RecordPlayerPointsRequest(1L, List.of(
                new PlayerPointsRequest(7L, 10), new PlayerPointsRequest(7L, 20))))))
                .containsExactly("eachPlayerListedOnce");
    }

    private static <T> Set<String> fieldsOf(Set<ConstraintViolation<T>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}

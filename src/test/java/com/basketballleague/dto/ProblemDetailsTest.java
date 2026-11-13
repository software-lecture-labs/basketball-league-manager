package com.basketballleague.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The JSON tests use Yasson with its default settings, as Payara's Jersey does, so the expected strings are
 * the response bodies shown in ADR-0006.
 */
class ProblemDetailsTest {

    private static Jsonb jsonb;

    @BeforeAll
    static void createJsonb() {
        jsonb = JsonbBuilder.create();
    }

    @AfterAll
    static void closeJsonb() throws Exception {
        jsonb.close();
    }

    @Test
    void copiesTheErrorListSoLaterChangesDoNotLeakIn() {
        final List<FieldViolation> errors = new ArrayList<>(List.of(new FieldViolation("name", "boş bırakılamaz")));

        final ProblemDetails problem = new ProblemDetails("about:blank", "Bad Request", 400, "x", "CODE", errors);
        errors.clear();

        assertThat(problem.errors()).hasSize(1);
        assertThatThrownBy(() -> problem.errors().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void leavesErrorsOutOfTheJsonWhenThereAreNone() {
        final ProblemDetails problem = new ProblemDetails("about:blank", "Conflict", 409,
                "Fikstür için birbirinden farklı en az iki takım gerekir.", "INVALID_FIXTURE_INPUT", null);

        assertThat(jsonb.toJson(problem)).isEqualTo("""
                {"type":"about:blank","title":"Conflict","status":409,\
                "detail":"Fikstür için birbirinden farklı en az iki takım gerekir.",\
                "code":"INVALID_FIXTURE_INPUT"}""");
    }

    @Test
    void writesTheFieldsInTheDocumentedOrderWithTheErrorList() {
        final ProblemDetails problem = new ProblemDetails("about:blank", "Bad Request", 400,
                "İstekte geçersiz alanlar var.", "VALIDATION_FAILED",
                List.of(new FieldViolation("name", "boş bırakılamaz"),
                        new FieldViolation("players[1].jerseyNumber", "99 veya daha küçük olmalıdır")));

        assertThat(jsonb.toJson(problem)).isEqualTo("""
                {"type":"about:blank","title":"Bad Request","status":400,\
                "detail":"İstekte geçersiz alanlar var.","code":"VALIDATION_FAILED",\
                "errors":[{"field":"name","message":"boş bırakılamaz"},\
                {"field":"players[1].jerseyNumber","message":"99 veya daha küçük olmalıdır"}]}""");
    }
}

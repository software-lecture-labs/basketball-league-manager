package com.basketballleague.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.io.Serial;
import org.junit.jupiter.api.Test;

class DomainExceptionTest {

    @Test
    void carriesItsErrorCodeAndKeepsTheMessageForTheLog() {
        final InvalidFixtureInputException exception = new InvalidFixtureInputException("At least two teams");

        assertThat(exception.code()).isEqualTo(ErrorCode.INVALID_FIXTURE_INPUT);
        assertThat(exception).hasMessage("At least two teams");
    }

    @Test
    void rejectsAMissingErrorCode() {
        assertThatNullPointerException()
                .isThrownBy(() -> new CodelessException())
                .withMessage("code");
    }

    private static final class CodelessException extends DomainException {

        @Serial
        private static final long serialVersionUID = 1L;

        CodelessException() {
            super(null, "no code");
        }
    }
}

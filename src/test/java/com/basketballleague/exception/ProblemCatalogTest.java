package com.basketballleague.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.basketballleague.domain.ErrorCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ProblemCatalogTest {

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    void everyErrorCodeHasAClientText(ErrorCode code) {
        assertThat(ProblemCatalog.detailFor(code)).isNotBlank();
    }
}

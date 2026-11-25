package com.basketballleague.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.basketballleague.domain.ErrorCode;
import com.basketballleague.domain.InvalidQuarterScoreException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class QuarterScoreTest {

    @Test
    void createsValidRegulationAndOvertimeQuartersIncludingZeroAndMaxPoints() {
        final QuarterScore first = new QuarterScore(1, 0, 0);
        final QuarterScore fourth = new QuarterScore(4, 24, 19);
        final QuarterScore overtime = new QuarterScore(5, Integer.MAX_VALUE, 10);
        final QuarterScore maxQuarter = new QuarterScore(Integer.MAX_VALUE, 5, Integer.MAX_VALUE);

        assertThat(first.getQuarterNo()).isEqualTo(1);
        assertThat(first.getHomePoints()).isZero();
        assertThat(first.getAwayPoints()).isZero();
        assertThat(first.isOvertime()).isFalse();

        assertThat(fourth.isOvertime()).isFalse();
        assertThat(overtime.isOvertime()).isTrue();
        assertThat(maxQuarter.getQuarterNo()).isEqualTo(Integer.MAX_VALUE);
        assertThat(maxQuarter.isOvertime()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void rejectsQuarterNumberBeforeTheFirst(int quarterNo) {
        assertThatThrownBy(() -> new QuarterScore(quarterNo, 20, 18))
                .isInstanceOf(InvalidQuarterScoreException.class)
                .extracting("code").isEqualTo(ErrorCode.INVALID_QUARTER_SCORE);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, Integer.MIN_VALUE})
    void rejectsNegativePointsForEitherSide(int points) {
        assertThatThrownBy(() -> new QuarterScore(1, points, 18))
                .isInstanceOf(InvalidQuarterScoreException.class)
                .extracting("code").isEqualTo(ErrorCode.INVALID_QUARTER_SCORE);
        assertThatThrownBy(() -> new QuarterScore(1, 20, points))
                .isInstanceOf(InvalidQuarterScoreException.class)
                .extracting("code").isEqualTo(ErrorCode.INVALID_QUARTER_SCORE);
    }

    @Test
    void comparesByAllValueFields() {
        final QuarterScore score = new QuarterScore(2, 21, 19);

        assertThat(score)
                .isEqualTo(new QuarterScore(2, 21, 19))
                .hasSameHashCodeAs(new QuarterScore(2, 21, 19))
                .isNotEqualTo(new QuarterScore(3, 21, 19))
                .isNotEqualTo(new QuarterScore(2, 22, 19))
                .isNotEqualTo(new QuarterScore(2, 21, 20))
                .isNotEqualTo(null)
                .isNotEqualTo("Q2");
        assertThat(score).hasToString("QuarterScore{quarterNo=2, homePoints=21, awayPoints=19}");
    }
}

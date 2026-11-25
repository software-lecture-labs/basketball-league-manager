package com.basketballleague.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.basketballleague.domain.ErrorCode;
import com.basketballleague.domain.InvalidGameException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PlayerPointsTest {

    @Test
    void createsValidPlayerPointsIncludingZeroAndMaxBoundaries() {
        final PlayerPoints scoreless = new PlayerPoints(1L, 10L, 0);
        final PlayerPoints maxBoundary = new PlayerPoints(Long.MAX_VALUE, Long.MAX_VALUE, Integer.MAX_VALUE);

        assertThat(scoreless.getPlayerId()).isEqualTo(1L);
        assertThat(scoreless.getTeamId()).isEqualTo(10L);
        assertThat(scoreless.getPoints()).isZero();
        assertThat(maxBoundary.getPoints()).isEqualTo(Integer.MAX_VALUE);
    }

    @ParameterizedTest
    @CsvSource({"0, 10", "-1, 10", "-9223372036854775808, 10", "5, 0", "5, -1", "5, -9223372036854775808"})
    void rejectsNonPositiveIdentifiers(long playerId, long teamId) {
        assertThatThrownBy(() -> new PlayerPoints(playerId, teamId, 10))
                .isInstanceOf(InvalidGameException.class)
                .extracting("code").isEqualTo(ErrorCode.INVALID_GAME);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, Integer.MIN_VALUE})
    void rejectsNegativePoints(int points) {
        assertThatThrownBy(() -> new PlayerPoints(5L, 10L, points))
                .isInstanceOf(InvalidGameException.class)
                .extracting("code").isEqualTo(ErrorCode.INVALID_GAME);
    }

    @Test
    void comparesByValueFields() {
        final PlayerPoints entry = new PlayerPoints(7L, 10L, 24);

        assertThat(entry)
                .isEqualTo(new PlayerPoints(7L, 10L, 24))
                .hasSameHashCodeAs(new PlayerPoints(7L, 10L, 24))
                .isNotEqualTo(new PlayerPoints(8L, 10L, 24))
                .isNotEqualTo(new PlayerPoints(7L, 20L, 24))
                .isNotEqualTo(new PlayerPoints(7L, 10L, 25))
                .isNotEqualTo(null)
                .isNotEqualTo("stat");
        assertThat(entry).hasToString("PlayerPoints{playerId=7, teamId=10, points=24}");
    }
}

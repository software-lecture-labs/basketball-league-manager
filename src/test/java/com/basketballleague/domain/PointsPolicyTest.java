package com.basketballleague.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PointsPolicyTest {

    private final PointsPolicy standardPolicy = StandardPointsPolicy.INSTANCE;

    @ParameterizedTest(name = "{0} wins, {1} losses should yield {2} points")
    @CsvSource({
            "0, 0, 0",
            "1, 0, 2",
            "0, 1, 1",
            "8, 2, 18",
            "10, 0, 20",
            "0, 10, 10",
            "17, 17, 51",
            "34, 0, 68",
            "25, 9, 59"
    })
    void standardPolicyCalculatesCorrectPoints(int wins, int losses, int expectedPoints) {
        assertThat(standardPolicy.calculatePoints(wins, losses)).isEqualTo(expectedPoints);
    }

    @Test
    void standardPolicyHasConstantValues() {
        assertThat(standardPolicy.pointsForWin()).isEqualTo(2);
        assertThat(standardPolicy.pointsForLoss()).isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({
            "-1, 0",
            "0, -1",
            "-5, -3"
    })
    void rejectsNegativeWinsOrLosses(int wins, int losses) {
        assertThatThrownBy(() -> standardPolicy.calculatePoints(wins, losses))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void customPointsPolicyCalculatesConfiguredPoints() {
        final PointsPolicy custom = new StandardPointsPolicy(3, 0);

        assertThat(custom.pointsForWin()).isEqualTo(3);
        assertThat(custom.pointsForLoss()).isEqualTo(0);
        assertThat(custom.calculatePoints(5, 2)).isEqualTo(15);
    }

    @Test
    void customPolicyRejectsNegativeConfiguration() {
        assertThatThrownBy(() -> new StandardPointsPolicy(-1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StandardPointsPolicy(2, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

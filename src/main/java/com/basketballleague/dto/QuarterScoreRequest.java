package com.basketballleague.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * One quarter in {@code PUT /games/{id}/quarters}. The upper limit is far above a real quarter and keeps game
 * totals away from int overflow; order and overtime rules are checked by {@code Game}.
 */
public record QuarterScoreRequest(
        @NotNull @Positive Integer quarterNo,
        @NotNull @PositiveOrZero @Max(MAX_QUARTER_POINTS) Integer homePoints,
        @NotNull @PositiveOrZero @Max(MAX_QUARTER_POINTS) Integer awayPoints) {

    static final int MAX_QUARTER_POINTS = 100;
}

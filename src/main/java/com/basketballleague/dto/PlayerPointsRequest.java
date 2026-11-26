package com.basketballleague.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * One player in {@code PUT /games/{id}/stats}. The team is not sent; the server takes the player's current team.
 */
public record PlayerPointsRequest(
        @NotNull @Positive Long playerId,
        @NotNull @PositiveOrZero @Max(MAX_PLAYER_POINTS) Integer points) {

    static final int MAX_PLAYER_POINTS = 200;
}

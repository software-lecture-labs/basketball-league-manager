package com.basketballleague.dto;

import jakarta.json.bind.annotation.JsonbPropertyOrder;

/**
 * A player's points in a game. {@code teamId} is the player's team when the points were recorded.
 */
@JsonbPropertyOrder({"playerId", "teamId", "points"})
public record PlayerPointsResponse(long playerId, long teamId, int points) {
}

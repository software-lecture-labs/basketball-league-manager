package com.basketballleague.dto;

import jakarta.json.bind.annotation.JsonbPropertyOrder;

/**
 * A game of a fixture. {@code status} is one of SCHEDULED, LIVE, FINISHED, CONFIRMED.
 */
@JsonbPropertyOrder({"id", "homeTeam", "awayTeam", "status"})
public record GameResponse(long id, TeamResponse homeTeam, TeamResponse awayTeam, String status) {
}

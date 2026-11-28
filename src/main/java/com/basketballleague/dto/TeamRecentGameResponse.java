package com.basketballleague.dto;

import jakarta.json.bind.annotation.JsonbPropertyOrder;

/**
 * A confirmed game in a team's recent season history.
 */
@JsonbPropertyOrder({"id", "roundNumber", "homeTeam", "awayTeam", "homeScore", "awayScore"})
public record TeamRecentGameResponse(
        long id,
        int roundNumber,
        TeamResponse homeTeam,
        TeamResponse awayTeam,
        int homeScore,
        int awayScore) {
}

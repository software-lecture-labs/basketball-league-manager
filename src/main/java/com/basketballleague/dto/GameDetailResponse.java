package com.basketballleague.dto;

import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.util.List;

/**
 * A game with its result, as {@code GET /games/{id}} returns it. Scores are {@code null} until the game is
 * finished. The client sends {@code version} back with its next change (ADR-0004).
 */
@JsonbPropertyOrder({"id", "seasonId", "roundNumber", "homeTeam", "awayTeam", "status", "homeScore", "awayScore",
        "quarters", "playerPoints", "version"})
public record GameDetailResponse(
        long id,
        long seasonId,
        int roundNumber,
        TeamResponse homeTeam,
        TeamResponse awayTeam,
        String status,
        Integer homeScore,
        Integer awayScore,
        List<QuarterScoreResponse> quarters,
        List<PlayerPointsResponse> playerPoints,
        long version) {
}

package com.basketballleague.dto;

import jakarta.json.bind.annotation.JsonbPropertyOrder;

/**
 * One row of {@code GET /api/seasons/{id}/standings}. Computed from confirmed games on every read; nothing is
 * stored separately (ADR-0001). {@code rank} starts at 1; {@code leaguePoints} is 2 per win and 1 per loss.
 */
@JsonbPropertyOrder({"rank", "teamId", "teamName", "played", "won", "lost", "pointsFor", "pointsAgainst",
        "pointDiff", "leaguePoints"})
public record StandingResponse(
        int rank,
        long teamId,
        String teamName,
        int played,
        int won,
        int lost,
        int pointsFor,
        int pointsAgainst,
        int pointDiff,
        int leaguePoints) {
}

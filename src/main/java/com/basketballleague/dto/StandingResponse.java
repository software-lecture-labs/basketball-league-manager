package com.basketballleague.dto;

import jakarta.json.bind.annotation.JsonbPropertyOrder;

/**
 * A single row in the league standings table as returned by the REST API.
 */
@JsonbPropertyOrder({
        "rank",
        "teamId",
        "teamName",
        "played",
        "won",
        "lost",
        "pointsScored",
        "pointsConceded",
        "pointDifference",
        "points"
})
public record StandingResponse(
        int rank,
        long teamId,
        String teamName,
        int played,
        int won,
        int lost,
        int pointsScored,
        int pointsConceded,
        int pointDifference,
        int points
) {
}

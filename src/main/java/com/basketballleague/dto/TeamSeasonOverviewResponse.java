package com.basketballleague.dto;

import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.util.List;

/**
 * One team's standings row and most recent confirmed games for a season.
 */
@JsonbPropertyOrder({"season", "summary", "lastFiveGames"})
public record TeamSeasonOverviewResponse(
        SeasonResponse season,
        StandingResponse summary,
        List<TeamRecentGameResponse> lastFiveGames) {

    public TeamSeasonOverviewResponse {
        lastFiveGames = List.copyOf(lastFiveGames);
    }
}

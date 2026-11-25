package com.basketballleague.dto;

import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.util.List;

/**
 * One round of a fixture. {@code byeTeam} is null, and left out of the JSON, when no team has a bye.
 */
@JsonbPropertyOrder({"number", "games", "byeTeam"})
public record RoundResponse(int number, List<GameResponse> games, TeamResponse byeTeam) {

    public RoundResponse {
        games = List.copyOf(games);
    }
}

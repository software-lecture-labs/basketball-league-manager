package com.basketballleague.dto;

import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.util.List;

/**
 * The fixture of a season, rounds in order. {@code rounds} is empty until the fixture is generated.
 */
@JsonbPropertyOrder({"seasonId", "rounds"})
public record FixtureResponse(long seasonId, List<RoundResponse> rounds) {

    public FixtureResponse {
        rounds = List.copyOf(rounds);
    }
}

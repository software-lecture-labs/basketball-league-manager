package com.basketballleague.domain;

import java.util.List;

/**
 * {@code byeTeamId} is null when no team has a bye.
 */
public record Round(int number, List<Pairing> pairings, Long byeTeamId) {

    public Round {
        if (number < 1) {
            throw new InvalidFixtureInputException("Round number must be at least one.");
        }
        pairings = List.copyOf(pairings);
    }
}

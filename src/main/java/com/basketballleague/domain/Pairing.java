package com.basketballleague.domain;

public record Pairing(long homeTeamId, long awayTeamId) {

    public Pairing {
        if (homeTeamId == awayTeamId) {
            throw new InvalidFixtureInputException("A team cannot play against itself.");
        }
    }
}

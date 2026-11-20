package com.basketballleague.domain;

import java.util.List;

public record Fixture(List<Round> rounds) {

    public Fixture {
        rounds = List.copyOf(rounds);
    }
}

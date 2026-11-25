package com.basketballleague.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class FixtureGenerator {

    private static final int TEAMS_PER_MATCH = 2;
    private static final int FIRST_ROTATING_INDEX = 1;
    private static final int ROTATION_STEP = 1;

    public Fixture generate(List<Long> teamIds) {
        validate(teamIds);
        final List<Long> circle = new ArrayList<>(teamIds);
        if (isOdd(circle.size())) {
            // A fixed bye keeps every real team in the balanced rotating positions.
            circle.add(0, null);
        }

        final int roundsPerHalf = circle.size() - 1;
        final List<Round> rounds = new ArrayList<>();
        for (int roundIndex = 0; roundIndex < roundsPerHalf; roundIndex++) {
            rounds.add(createRound(circle, roundIndex));
            Collections.rotate(circle.subList(FIRST_ROTATING_INDEX, circle.size()), ROTATION_STEP);
        }
        for (int roundIndex = 0; roundIndex < roundsPerHalf; roundIndex++) {
            final Round firstHalfRound = rounds.get(roundIndex);
            final List<Pairing> reversed = firstHalfRound.pairings().stream()
                    .map(pairing -> new Pairing(pairing.awayTeamId(), pairing.homeTeamId()))
                    .toList();
            rounds.add(new Round(roundsPerHalf + firstHalfRound.number(), reversed, firstHalfRound.byeTeamId()));
        }
        return new Fixture(rounds);
    }

    private void validate(List<Long> teamIds) {
        if (teamIds == null || teamIds.size() < TEAMS_PER_MATCH) {
            throw new InvalidFixtureInputException("At least two teams are required.");
        }
        final Set<Long> uniqueIds = new HashSet<>();
        for (Long teamId : teamIds) {
            if (teamId == null || !uniqueIds.add(teamId)) {
                throw new InvalidFixtureInputException("Team identifiers must be non-null and unique.");
            }
        }
    }

    private Round createRound(List<Long> circle, int roundIndex) {
        final List<Pairing> pairings = new ArrayList<>();
        Long byeTeamId = null;
        for (int pairIndex = 0; pairIndex < circle.size() / TEAMS_PER_MATCH; pairIndex++) {
            final Long left = circle.get(pairIndex);
            final Long right = circle.get(circle.size() - 1 - pairIndex);
            if (left == null || right == null) {
                byeTeamId = left == null ? right : left;
            } else {
                // Alternate the fixed team's venue by round and the other pairs' venues by position.
                final boolean reverse = pairIndex == 0 ? isOdd(roundIndex) : isOdd(pairIndex);
                pairings.add(reverse ? new Pairing(right, left) : new Pairing(left, right));
            }
        }
        return new Round(roundIndex + 1, pairings, byeTeamId);
    }

    private boolean isOdd(int value) {
        return value % 2 != 0;
    }
}

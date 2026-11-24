package com.basketballleague.domain;

import java.util.Set;

/**
 * Life cycle of a game: {@code SCHEDULED → LIVE → FINISHED → CONFIRMED}. A confirmed game is final; its score
 * no longer changes (ADR-0003).
 */
public enum GameStatus {
    SCHEDULED,
    LIVE,
    FINISHED,
    CONFIRMED;

    // No default branch: a new status without its transitions stops the build.
    public Set<GameStatus> allowedTransitions() {
        return switch (this) {
            case SCHEDULED -> Set.of(LIVE);
            case LIVE -> Set.of(FINISHED);
            case FINISHED -> Set.of(CONFIRMED);
            case CONFIRMED -> Set.of();
        };
    }

    public boolean canTransitionTo(GameStatus target) {
        return allowedTransitions().contains(target);
    }

    public boolean isTerminal() {
        return allowedTransitions().isEmpty();
    }
}

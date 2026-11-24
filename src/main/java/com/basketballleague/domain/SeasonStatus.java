package com.basketballleague.domain;

import java.util.Set;

/**
 * Life cycle of a season: {@code DRAFT → ACTIVE → FINISHED}. Teams join only a draft season; generating the
 * fixture starts it (ADR-0003).
 */
public enum SeasonStatus {
    DRAFT,
    ACTIVE,
    FINISHED;

    // No default branch: a new status without its transitions stops the build.
    public Set<SeasonStatus> allowedTransitions() {
        return switch (this) {
            case DRAFT -> Set.of(ACTIVE);
            case ACTIVE -> Set.of(FINISHED);
            case FINISHED -> Set.of();
        };
    }

    public boolean canTransitionTo(SeasonStatus target) {
        return allowedTransitions().contains(target);
    }

    public boolean isTerminal() {
        return allowedTransitions().isEmpty();
    }

    /** Only a draft season accepts teams; once the fixture exists, the team list is fixed. */
    public boolean acceptsTeams() {
        return this == DRAFT;
    }
}

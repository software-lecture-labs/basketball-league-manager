package com.basketballleague.domain;

import java.io.Serial;

/**
 * The team has already joined the season (409).
 */
public final class TeamAlreadyInSeasonException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public TeamAlreadyInSeasonException(Long seasonId, long teamId) {
        super(ErrorCode.TEAM_ALREADY_IN_SEASON, "Team " + teamId + " is already in season " + seasonId);
    }
}

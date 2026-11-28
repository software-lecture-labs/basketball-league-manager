package com.basketballleague.domain;

import java.io.Serial;

/**
 * The requested team is not registered in the selected season (404).
 */
public final class TeamNotInSeasonException extends RecordNotFoundException {

    @Serial
    private static final long serialVersionUID = 1L;

    public TeamNotInSeasonException(long teamId, long seasonId) {
        super(ErrorCode.TEAM_NOT_IN_SEASON,
                "Team " + teamId + " does not participate in season " + seasonId);
    }
}

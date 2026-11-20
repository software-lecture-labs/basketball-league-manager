package com.basketballleague.domain;

import java.io.Serial;

/**
 * No team exists with the requested id (404).
 */
public final class TeamNotFoundException extends RecordNotFoundException {

    @Serial
    private static final long serialVersionUID = 1L;

    public TeamNotFoundException(long teamId) {
        super(ErrorCode.TEAM_NOT_FOUND, "Team " + teamId + " does not exist");
    }
}

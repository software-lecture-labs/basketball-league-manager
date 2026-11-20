package com.basketballleague.domain;

import java.io.Serial;

/**
 * Another team already uses the requested name (409).
 */
public final class TeamNameTakenException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public TeamNameTakenException(String name) {
        super(ErrorCode.TEAM_NAME_TAKEN, "Team name already in use: " + name);
    }
}

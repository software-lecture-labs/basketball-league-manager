package com.basketballleague.domain;

import java.io.Serial;

/**
 * No season exists with the requested id (404).
 */
public final class SeasonNotFoundException extends RecordNotFoundException {

    @Serial
    private static final long serialVersionUID = 1L;

    public SeasonNotFoundException(long seasonId) {
        super(ErrorCode.SEASON_NOT_FOUND, "Season " + seasonId + " does not exist");
    }
}

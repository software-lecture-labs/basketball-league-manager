package com.basketballleague.domain;

import java.io.Serial;

/**
 * Another season already uses the requested name (409).
 */
public final class SeasonNameTakenException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public SeasonNameTakenException(String name) {
        super(ErrorCode.SEASON_NAME_TAKEN, "Season name is taken: " + name);
    }
}

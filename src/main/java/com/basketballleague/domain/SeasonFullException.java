package com.basketballleague.domain;

import java.io.Serial;

/**
 * The season already has the largest number of teams allowed (409).
 */
public final class SeasonFullException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public SeasonFullException(Long seasonId, int maxTeams) {
        super(ErrorCode.SEASON_FULL, "Season " + seasonId + " already has " + maxTeams + " teams");
    }
}

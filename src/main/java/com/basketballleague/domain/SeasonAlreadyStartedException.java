package com.basketballleague.domain;

import java.io.Serial;

/**
 * The season is no longer a draft, so its team list and fixture are fixed (409). Covers both adding a team to a
 * started season and generating the fixture a second time (ADR-0003).
 */
public final class SeasonAlreadyStartedException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public SeasonAlreadyStartedException(Long seasonId, SeasonStatus status) {
        super(ErrorCode.SEASON_ALREADY_STARTED, "Season " + seasonId + " is " + status + ", not DRAFT");
    }
}

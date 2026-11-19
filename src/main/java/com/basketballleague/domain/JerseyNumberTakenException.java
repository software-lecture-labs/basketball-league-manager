package com.basketballleague.domain;

import java.io.Serial;

/**
 * Another player of the same team already wears the requested jersey number (409).
 */
public final class JerseyNumberTakenException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public JerseyNumberTakenException(long teamId, int jerseyNumber) {
        super(ErrorCode.JERSEY_NUMBER_TAKEN, "Jersey number " + jerseyNumber + " is taken in team " + teamId);
    }
}

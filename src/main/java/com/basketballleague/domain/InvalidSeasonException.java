package com.basketballleague.domain;

import java.io.Serial;

/**
 * A season would be created or changed into a state the business rules do not allow.
 */
public final class InvalidSeasonException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidSeasonException(String message) {
        super(ErrorCode.INVALID_SEASON, message);
    }
}

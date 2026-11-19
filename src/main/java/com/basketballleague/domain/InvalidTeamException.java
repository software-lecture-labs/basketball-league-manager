package com.basketballleague.domain;

import java.io.Serial;

/**
 * A team would be created or changed into a state the business rules do not allow.
 */
public final class InvalidTeamException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidTeamException(String message) {
        super(ErrorCode.INVALID_TEAM, message);
    }
}

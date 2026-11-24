package com.basketballleague.domain;

import java.io.Serial;

/**
 * A game would be created with values the business rules do not allow.
 */
public final class InvalidGameException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidGameException(String message) {
        super(ErrorCode.INVALID_GAME, message);
    }
}

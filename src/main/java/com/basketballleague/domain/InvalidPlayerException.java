package com.basketballleague.domain;

import java.io.Serial;

/**
 * A player would be created or changed into a state the business rules do not allow.
 */
public final class InvalidPlayerException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidPlayerException(String message) {
        super(ErrorCode.INVALID_PLAYER, message);
    }
}

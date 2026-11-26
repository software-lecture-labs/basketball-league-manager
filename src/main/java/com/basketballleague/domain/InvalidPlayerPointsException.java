package com.basketballleague.domain;

import java.io.Serial;

/**
 * A player points entry is missing, has a non-positive id, negative points or repeats a player (409).
 */
public final class InvalidPlayerPointsException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidPlayerPointsException(String message) {
        super(ErrorCode.INVALID_PLAYER_POINTS, message);
    }
}

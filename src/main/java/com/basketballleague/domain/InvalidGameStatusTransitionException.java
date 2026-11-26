package com.basketballleague.domain;

import java.io.Serial;

/**
 * A game was asked to move to a status its current status does not allow, such as
 * {@code SCHEDULED → CONFIRMED} (409).
 */
public final class InvalidGameStatusTransitionException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidGameStatusTransitionException(Long gameId, GameStatus from, GameStatus to) {
        super(ErrorCode.INVALID_GAME_STATUS_TRANSITION,
                "Game " + gameId + " cannot move from " + from + " to " + to);
    }

    public InvalidGameStatusTransitionException(Long gameId, GameStatus current, String action) {
        super(ErrorCode.INVALID_GAME_STATUS_TRANSITION,
                "Game " + gameId + " cannot " + action + " while " + current);
    }
}

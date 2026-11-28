package com.basketballleague.domain;

import java.io.Serial;

/**
 * A player stat references a player or team not participating in the game (409).
 */
public final class PlayerNotInGameException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public PlayerNotInGameException(long playerId, Long gameId, long teamId) {
        super(ErrorCode.PLAYER_NOT_IN_GAME,
                "Player " + playerId + " on team " + teamId + " does not belong to game " + gameId);
    }

    /** The player id does not exist at all; the client sees the same code as for a player of another team. */
    public PlayerNotInGameException(long playerId, Long gameId) {
        super(ErrorCode.PLAYER_NOT_IN_GAME, "Player " + playerId + " does not exist (game " + gameId + ")");
    }
}

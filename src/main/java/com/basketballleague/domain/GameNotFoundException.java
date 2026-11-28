package com.basketballleague.domain;

import java.io.Serial;

/**
 * No game exists with the requested id (404).
 */
public final class GameNotFoundException extends RecordNotFoundException {

    @Serial
    private static final long serialVersionUID = 1L;

    public GameNotFoundException(long gameId) {
        super(ErrorCode.GAME_NOT_FOUND, "Game " + gameId + " does not exist");
    }
}

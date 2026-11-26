package com.basketballleague.domain;

import java.io.Serial;

/**
 * Player point totals or quarter totals do not match the game's recorded team scores (409).
 */
public final class ScoreMismatchException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ScoreMismatchException(String message) {
        super(ErrorCode.SCORE_MISMATCH, message);
    }

    public ScoreMismatchException(Long gameId, int expectedHome, int actualHome, int expectedAway, int actualAway) {
        super(ErrorCode.SCORE_MISMATCH,
                "Game " + gameId + " player totals (" + actualHome + "-" + actualAway
                        + ") do not match team scores (" + expectedHome + "-" + expectedAway + ")");
    }
}

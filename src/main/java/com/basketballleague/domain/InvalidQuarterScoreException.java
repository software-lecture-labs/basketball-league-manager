package com.basketballleague.domain;

import java.io.Serial;

/**
 * Quarter scores are out of sequence, negative, incomplete on finish, or include overtime without a tie (409).
 */
public final class InvalidQuarterScoreException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidQuarterScoreException(String message) {
        super(ErrorCode.INVALID_QUARTER_SCORE, message);
    }
}

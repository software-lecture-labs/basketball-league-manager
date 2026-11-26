package com.basketballleague.domain;

import java.io.Serial;

/**
 * A game ended tied after regulation or the latest overtime period; another overtime quarter is required (409).
 */
public final class OvertimeRequiredException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public OvertimeRequiredException(Long gameId, int tiedScore) {
        super(ErrorCode.OVERTIME_REQUIRED,
                "Game " + gameId + " cannot finish tied at " + tiedScore + "-" + tiedScore);
    }
}

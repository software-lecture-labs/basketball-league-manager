package com.basketballleague.domain;

import java.io.Serial;

public final class InvalidFixtureInputException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidFixtureInputException(String message) {
        super(ErrorCode.INVALID_FIXTURE_INPUT, message);
    }
}

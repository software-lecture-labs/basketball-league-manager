package com.basketballleague.domain;

import java.io.Serial;

/**
 * Base type for "the requested record does not exist". The REST layer answers it with 404 instead of the 409
 * used for other business rule violations.
 */
public abstract class RecordNotFoundException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    protected RecordNotFoundException(ErrorCode code, String message) {
        super(code, message);
    }
}

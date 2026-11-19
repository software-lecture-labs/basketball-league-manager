package com.basketballleague.domain;

import java.io.Serial;
import java.util.Objects;

public abstract class DomainException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ErrorCode code;

    /**
     * {@code message} is written to the server log only; the client receives a text chosen by {@code code}.
     */
    protected DomainException(ErrorCode code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
    }

    public ErrorCode code() {
        return code;
    }
}

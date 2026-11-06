package com.basketballleague.domain;

import java.io.Serial;

public abstract class DomainException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    protected DomainException(String message) {
        super(message);
    }
}

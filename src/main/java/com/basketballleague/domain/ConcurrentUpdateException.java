package com.basketballleague.domain;

import java.io.Serial;

/**
 * The caller's entity version is stale; another change has already been committed (409, ADR-0004).
 */
public final class ConcurrentUpdateException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ConcurrentUpdateException(Long gameId, long expectedVersion, long currentVersion) {
        super(ErrorCode.CONCURRENT_UPDATE,
                "Game " + gameId + " expected version " + expectedVersion + " but was " + currentVersion);
    }
}

package com.basketballleague.exception;

import jakarta.persistence.OptimisticLockException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.Set;

/**
 * Recognises a concurrent update or a data integrity violation anywhere in a failure's cause chain. Such failures
 * often surface at commit time, wrapped by the transaction manager or the {@code @Transactional} interceptor, and
 * the wrapper type depends on the server (ADR-0006, "Sarılmış istisnalar").
 */
final class PersistenceConflicts {

    /** SQLState class 23: integrity constraint violation (unique, foreign key, check, not null). */
    private static final String INTEGRITY_VIOLATION_CLASS = "23";

    private PersistenceConflicts() {
    }

    static Optional<GeneralProblem> classify(Throwable failure) {
        final Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = failure; cause != null && seen.add(cause); cause = cause.getCause()) {
            if (cause instanceof OptimisticLockException) {
                return Optional.of(GeneralProblem.CONCURRENT_UPDATE);
            }
            if (cause instanceof SQLException sqlException && isIntegrityViolation(sqlException)) {
                return Optional.of(GeneralProblem.DATA_CONFLICT);
            }
        }
        return Optional.empty();
    }

    private static boolean isIntegrityViolation(SQLException exception) {
        final String state = exception.getSQLState();
        return state != null && state.startsWith(INTEGRITY_VIOLATION_CLASS);
    }
}

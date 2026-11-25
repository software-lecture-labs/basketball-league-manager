package com.basketballleague.exception;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PersistenceConflictsTest {

    @Test
    void recognisesAnOptimisticLockFailureThrownDirectly() {
        assertThat(PersistenceConflicts.classify(new OptimisticLockException("version 3 expected")))
                .contains(GeneralProblem.CONCURRENT_UPDATE);
    }

    @Test
    void recognisesAnOptimisticLockFailureWrappedAtCommit() {
        final RuntimeException commitFailure = new RuntimeException("transaction rolled back",
                new IllegalStateException("commit failed", new OptimisticLockException("stale game")));

        assertThat(PersistenceConflicts.classify(commitFailure)).contains(GeneralProblem.CONCURRENT_UPDATE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"23505", "23503", "23514", "23502"})
    void recognisesIntegrityViolationsBySqlStateClass(String sqlState) {
        final PersistenceException failure = new PersistenceException("flush failed",
                new SQLException("duplicate key value violates unique constraint \"uq_seasons_name\"", sqlState));

        assertThat(PersistenceConflicts.classify(failure)).contains(GeneralProblem.DATA_CONFLICT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"08006", "40001", "42P01"})
    void ignoresOtherDatabaseErrors(String sqlState) {
        assertThat(PersistenceConflicts.classify(new PersistenceException(new SQLException("failure", sqlState))))
                .isEmpty();
    }

    @Test
    void ignoresAnSqlExceptionWithoutState() {
        assertThat(PersistenceConflicts.classify(new SQLException("no state"))).isEmpty();
    }

    @Test
    void ignoresFailuresUnrelatedToPersistence() {
        assertThat(PersistenceConflicts.classify(new IllegalArgumentException("bug"))).isEmpty();
    }

    @Test
    void stopsAtACycleInTheCauseChain() {
        final RuntimeException first = new RuntimeException("first");
        final RuntimeException second = new RuntimeException("second", first);
        first.initCause(second);

        assertThat(PersistenceConflicts.classify(first)).isEmpty();
    }
}

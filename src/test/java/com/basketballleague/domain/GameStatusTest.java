package com.basketballleague.domain;

import static com.basketballleague.domain.GameStatus.CONFIRMED;
import static com.basketballleague.domain.GameStatus.FINISHED;
import static com.basketballleague.domain.GameStatus.LIVE;
import static com.basketballleague.domain.GameStatus.SCHEDULED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class GameStatusTest {

    /** The transition table of ADR-0003 and ADR-0004 (reopen), written out independently of the enum. */
    private static final Map<GameStatus, Set<GameStatus>> TABLE = Map.of(
            SCHEDULED, Set.of(LIVE),
            LIVE, Set.of(FINISHED),
            FINISHED, Set.of(LIVE, CONFIRMED),
            CONFIRMED, Set.of());

    @ParameterizedTest
    @EnumSource(GameStatus.class)
    void allowsExactlyTheTransitionsOfTheTable(GameStatus from) {
        for (GameStatus to : GameStatus.values()) {
            assertThat(from.canTransitionTo(to))
                    .as("%s -> %s", from, to)
                    .isEqualTo(TABLE.get(from).contains(to));
        }
    }

    @Test
    void scheduledGameCannotBeConfirmedDirectly() {
        assertThat(SCHEDULED.canTransitionTo(CONFIRMED)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(GameStatus.class)
    void noStatusMovesToItself(GameStatus status) {
        assertThat(status.canTransitionTo(status)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(GameStatus.class)
    void onlyConfirmedIsTerminal(GameStatus status) {
        assertThat(status.isTerminal()).isEqualTo(status == CONFIRMED);
    }

    @Test
    void allowedTransitionsCannotBeChangedByTheCaller() {
        final Set<GameStatus> transitions = SCHEDULED.allowedTransitions();

        assertThatThrownBy(transitions::clear).isInstanceOf(UnsupportedOperationException.class);
    }
}

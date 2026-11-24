package com.basketballleague.domain;

import static com.basketballleague.domain.SeasonStatus.ACTIVE;
import static com.basketballleague.domain.SeasonStatus.DRAFT;
import static com.basketballleague.domain.SeasonStatus.FINISHED;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SeasonStatusTest {

    /** The transition table of ADR-0003, written out independently of the enum. */
    private static final Map<SeasonStatus, Set<SeasonStatus>> TABLE = Map.of(
            DRAFT, Set.of(ACTIVE),
            ACTIVE, Set.of(FINISHED),
            FINISHED, Set.of());

    @ParameterizedTest
    @EnumSource(SeasonStatus.class)
    void allowsExactlyTheTransitionsOfTheTable(SeasonStatus from) {
        for (SeasonStatus to : SeasonStatus.values()) {
            assertThat(from.canTransitionTo(to))
                    .as("%s -> %s", from, to)
                    .isEqualTo(TABLE.get(from).contains(to));
        }
    }

    @ParameterizedTest
    @EnumSource(SeasonStatus.class)
    void onlyFinishedIsTerminal(SeasonStatus status) {
        assertThat(status.isTerminal()).isEqualTo(status == FINISHED);
    }

    @ParameterizedTest
    @EnumSource(SeasonStatus.class)
    void onlyADraftSeasonAcceptsTeams(SeasonStatus status) {
        assertThat(status.acceptsTeams()).isEqualTo(status == DRAFT);
    }
}

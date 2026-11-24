package com.basketballleague.entity;

import static com.basketballleague.support.EntityIds.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.basketballleague.domain.ErrorCode;
import com.basketballleague.domain.InvalidFixtureInputException;
import com.basketballleague.domain.InvalidSeasonException;
import com.basketballleague.domain.SeasonAlreadyStartedException;
import com.basketballleague.domain.SeasonFullException;
import com.basketballleague.domain.SeasonStatus;
import com.basketballleague.domain.TeamAlreadyInSeasonException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SeasonTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 1);

    private static Season draft() {
        return new Season("2026-27 Demo Sezonu", START);
    }

    private static Season withTeams(int count) {
        final Season season = draft();
        for (long teamId = 1; teamId <= count; teamId++) {
            season.addTeam(teamId);
        }
        return season;
    }

    @Test
    void newSeasonIsADraftWithoutTeams() {
        final Season season = new Season("  2026-27 Demo Sezonu ", START);

        assertThat(season.getName()).isEqualTo("2026-27 Demo Sezonu");
        assertThat(season.getStartDate()).isEqualTo(START);
        assertThat(season.getStatus()).isEqualTo(SeasonStatus.DRAFT);
        assertThat(season.getTeamIds()).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void rejectsABlankName(String name) {
        assertThatThrownBy(() -> new Season(name, START))
                .isInstanceOf(InvalidSeasonException.class)
                .extracting("code").isEqualTo(ErrorCode.INVALID_SEASON);
    }

    @Test
    void rejectsAMissingStartDate() {
        assertThatThrownBy(() -> new Season("2026-27", null)).isInstanceOf(InvalidSeasonException.class);
    }

    @Test
    void returnsTeamIdsInAscendingOrderWhateverTheJoinOrder() {
        final Season season = draft();
        season.addTeam(12L);
        season.addTeam(3L);
        season.addTeam(7L);

        assertThat(season.getTeamIds()).containsExactly(3L, 7L, 12L);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, Long.MIN_VALUE})
    void rejectsANonPositiveTeamId(long teamId) {
        assertThatThrownBy(() -> draft().addTeam(teamId)).isInstanceOf(InvalidSeasonException.class);
    }

    @Test
    void rejectsTheSameTeamTwice() {
        final Season season = draft();
        season.addTeam(5L);

        assertThatThrownBy(() -> season.addTeam(5L)).isInstanceOf(TeamAlreadyInSeasonException.class);
        assertThat(season.getTeamIds()).containsExactly(5L);
    }

    @Test
    void acceptsUpToTwentyTeamsAndRejectsTheTwentyFirst() {
        final Season season = withTeams(Season.MAX_TEAMS);

        assertThat(season.getTeamIds()).hasSize(Season.MAX_TEAMS);
        assertThatThrownBy(() -> season.addTeam(Season.MAX_TEAMS + 1L)).isInstanceOf(SeasonFullException.class);
    }

    @Test
    void activateStartsTheSeason() {
        final Season season = withTeams(Season.MIN_TEAMS);

        season.activate();

        assertThat(season.getStatus()).isEqualTo(SeasonStatus.ACTIVE);
    }

    @Test
    void activateNeedsAtLeastTwoTeamsAndLeavesTheSeasonAsDraft() {
        final Season season = withTeams(1);

        assertThatThrownBy(season::activate).isInstanceOf(InvalidFixtureInputException.class);
        assertThat(season.getStatus()).isEqualTo(SeasonStatus.DRAFT);
    }

    @Test
    void secondActivationIsRejectedSoTheFixtureCannotBeGeneratedTwice() {
        final Season season = withTeams(4);
        season.activate();

        assertThatThrownBy(season::activate)
                .isInstanceOf(SeasonAlreadyStartedException.class)
                .extracting("code").isEqualTo(ErrorCode.SEASON_ALREADY_STARTED);
    }

    @Test
    void startedSeasonDoesNotAcceptTeams() {
        final Season season = withTeams(4);
        season.activate();

        assertThatThrownBy(() -> season.addTeam(99L)).isInstanceOf(SeasonAlreadyStartedException.class);
        assertThat(season.getTeamIds()).hasSize(4);
    }

    @Test
    void teamIdsCannotBeChangedFromOutside() {
        final Season season = withTeams(2);

        assertThatThrownBy(() -> season.getTeamIds().add(3L)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void seasonsAreEqualOnlyWhenTheyHaveTheSameDatabaseId() {
        final Season stored = withId(draft(), 4L);

        assertThat(stored).isEqualTo(withId(new Season("Başka ad", START), 4L))
                .isNotEqualTo(withId(draft(), 5L))
                .isNotEqualTo(draft())
                .isNotEqualTo(null)
                .isNotEqualTo("2026-27 Demo Sezonu")
                .hasSameHashCodeAs(draft());
        assertThat(draft()).isNotEqualTo(draft());
    }

    @Test
    void toStringNamesTheSeasonForTheLog() {
        assertThat(withId(draft(), 4L))
                .hasToString("Season{id=4, name='2026-27 Demo Sezonu', status=DRAFT}");
    }
}

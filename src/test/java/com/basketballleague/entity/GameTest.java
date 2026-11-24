package com.basketballleague.entity;

import static com.basketballleague.support.EntityIds.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.basketballleague.domain.ErrorCode;
import com.basketballleague.domain.GameStatus;
import com.basketballleague.domain.InvalidGameException;
import com.basketballleague.domain.InvalidGameStatusTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class GameTest {

    private static final long SEASON_ID = 1L;
    private static final long HOME = 10L;
    private static final long AWAY = 20L;

    @Test
    void newGameIsScheduled() {
        final Game game = new Game(SEASON_ID, 3, HOME, AWAY);

        assertThat(game.getSeasonId()).isEqualTo(SEASON_ID);
        assertThat(game.getRoundNumber()).isEqualTo(3);
        assertThat(game.getHomeTeamId()).isEqualTo(HOME);
        assertThat(game.getAwayTeamId()).isEqualTo(AWAY);
        assertThat(game.getStatus()).isEqualTo(GameStatus.SCHEDULED);
    }

    @Test
    void rejectsATeamPlayingItself() {
        assertThatThrownBy(() -> new Game(SEASON_ID, 1, HOME, HOME))
                .isInstanceOf(InvalidGameException.class)
                .extracting("code").isEqualTo(ErrorCode.INVALID_GAME);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void rejectsARoundBeforeTheFirst(int roundNumber) {
        assertThatThrownBy(() -> new Game(SEASON_ID, roundNumber, HOME, AWAY))
                .isInstanceOf(InvalidGameException.class);
    }

    @Test
    void acceptsTheLargestRoundNumber() {
        assertThat(new Game(SEASON_ID, Integer.MAX_VALUE, HOME, AWAY).getRoundNumber())
                .isEqualTo(Integer.MAX_VALUE);
    }

    @ParameterizedTest
    @CsvSource({"0, 10, 20", "1, 0, 20", "1, 10, 0", "-1, 10, 20", "1, -10, 20", "1, 10, -20"})
    void rejectsNonPositiveIds(long seasonId, long homeTeamId, long awayTeamId) {
        assertThatThrownBy(() -> new Game(seasonId, 1, homeTeamId, awayTeamId))
                .isInstanceOf(InvalidGameException.class);
    }

    @Test
    void startMakesTheGameLive() {
        final Game game = new Game(SEASON_ID, 1, HOME, AWAY);

        game.start();

        assertThat(game.getStatus()).isEqualTo(GameStatus.LIVE);
    }

    @Test
    void liveGameCannotStartAgainAndKeepsItsStatus() {
        final Game game = new Game(SEASON_ID, 1, HOME, AWAY);
        game.start();

        assertThatThrownBy(game::start)
                .isInstanceOf(InvalidGameStatusTransitionException.class)
                .extracting("code").isEqualTo(ErrorCode.INVALID_GAME_STATUS_TRANSITION);
        assertThat(game.getStatus()).isEqualTo(GameStatus.LIVE);
    }

    @Test
    void gamesAreEqualOnlyWhenTheyHaveTheSameDatabaseId() {
        final Game stored = withId(new Game(SEASON_ID, 1, HOME, AWAY), 9L);

        assertThat(stored).isEqualTo(withId(new Game(SEASON_ID, 2, AWAY, HOME), 9L))
                .isNotEqualTo(withId(new Game(SEASON_ID, 1, HOME, AWAY), 10L))
                .isNotEqualTo(new Game(SEASON_ID, 1, HOME, AWAY))
                .isNotEqualTo(null)
                .isNotEqualTo("game")
                .hasSameHashCodeAs(new Game(SEASON_ID, 1, HOME, AWAY));
        assertThat(stored.getId()).isEqualTo(9L);
    }

    @Test
    void newGameStartsAtVersionZero() {
        assertThat(new Game(SEASON_ID, 1, HOME, AWAY).getVersion()).isZero();
    }

    @Test
    void toStringDescribesTheGameForTheLog() {
        assertThat(withId(new Game(SEASON_ID, 3, HOME, AWAY), 9L))
                .hasToString("Game{id=9, seasonId=1, round=3, home=10, away=20, status=SCHEDULED}");
    }
}

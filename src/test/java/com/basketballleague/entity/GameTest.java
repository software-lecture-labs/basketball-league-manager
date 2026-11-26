package com.basketballleague.entity;

import static com.basketballleague.support.EntityFields.withField;
import static com.basketballleague.support.EntityIds.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.basketballleague.domain.ConcurrentUpdateException;
import com.basketballleague.domain.ErrorCode;
import com.basketballleague.domain.GameStatus;
import com.basketballleague.domain.InvalidGameException;
import com.basketballleague.domain.InvalidGameStatusTransitionException;
import com.basketballleague.domain.InvalidPlayerPointsException;
import com.basketballleague.domain.InvalidQuarterScoreException;
import com.basketballleague.domain.OvertimeRequiredException;
import com.basketballleague.domain.PlayerNotInGameException;
import com.basketballleague.domain.ScoreMismatchException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class GameTest {

    private static final long SEASON_ID = 1L;
    private static final long HOME = 10L;
    private static final long AWAY = 20L;
    private static final long OTHER_TEAM = 30L;

    private static Game liveGame() {
        final Game game = withId(new Game(SEASON_ID, 1, HOME, AWAY), 42L);
        game.start();
        return game;
    }

    private static List<QuarterScore> fourRegulationQuarters() {
        return List.of(
                new QuarterScore(1, 22, 18),
                new QuarterScore(2, 20, 21),
                new QuarterScore(3, 19, 17),
                new QuarterScore(4, 24, 20));
    }

    private static List<QuarterScore> tiedRegulationQuarters() {
        return List.of(
                new QuarterScore(1, 20, 18),
                new QuarterScore(2, 18, 20),
                new QuarterScore(3, 21, 22),
                new QuarterScore(4, 21, 20));
    }

    private static Game finishedGame() {
        final Game game = liveGame();
        game.recordQuarters(fourRegulationQuarters());
        game.finish();
        return game;
    }

    private static List<PlayerPoints> matchingPlayerPoints() {
        return List.of(
                new PlayerPoints(101L, HOME, 45),
                new PlayerPoints(102L, HOME, 40),
                new PlayerPoints(201L, AWAY, 50),
                new PlayerPoints(202L, AWAY, 26),
                new PlayerPoints(203L, AWAY, 0));
    }

    private static Game withVersion(Game game, long version) {
        return withField(game, "version", version);
    }

    @Test
    void newGameIsScheduledWithNullScoresAndEmptyCollections() {
        final Game game = new Game(SEASON_ID, 3, HOME, AWAY);

        assertThat(game.getSeasonId()).isEqualTo(SEASON_ID);
        assertThat(game.getRoundNumber()).isEqualTo(3);
        assertThat(game.getHomeTeamId()).isEqualTo(HOME);
        assertThat(game.getAwayTeamId()).isEqualTo(AWAY);
        assertThat(game.getStatus()).isEqualTo(GameStatus.SCHEDULED);
        assertThat(game.getHomeScore()).isNull();
        assertThat(game.getAwayScore()).isNull();
        assertThat(game.getQuarterScores()).isEmpty();
        assertThat(game.getPlayerPoints()).isEmpty();
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
        final Game game = liveGame();

        assertThatThrownBy(game::start)
                .isInstanceOf(InvalidGameStatusTransitionException.class)
                .extracting("code").isEqualTo(ErrorCode.INVALID_GAME_STATUS_TRANSITION);
        assertThat(game.getStatus()).isEqualTo(GameStatus.LIVE);
    }

    @Test
    void finishedGameCannotRestartViaStartWithoutReopen() {
        final Game game = finishedGame();

        assertThatThrownBy(game::start)
                .isInstanceOf(InvalidGameStatusTransitionException.class)
                .extracting("code").isEqualTo(ErrorCode.INVALID_GAME_STATUS_TRANSITION);
        assertThat(game.getStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(game.getHomeScore()).isEqualTo(85);
    }

    @Test
    void recordsQuartersSequentiallyAndAllowsUpdatingAnExistingQuarter() {
        final Game game = liveGame();
        final QuarterScore q1 = new QuarterScore(1, 20, 18);
        final QuarterScore q2 = new QuarterScore(2, 15, 22);
        final QuarterScore q1Corrected = new QuarterScore(1, 22, 18);

        game.recordQuarter(q1);
        game.recordQuarter(q2);
        game.recordQuarter(q1Corrected);

        assertThat(game.getQuarterScores()).containsExactly(q1Corrected, q2);
    }

    @Test
    void replacesQuarterScoresAsWholeListAndIsolatesCallerList() {
        final Game game = liveGame();
        game.recordQuarter(new QuarterScore(1, 10, 10));

        final List<QuarterScore> callerList = new ArrayList<>(fourRegulationQuarters());
        game.recordQuarters(callerList);
        callerList.clear();

        assertThat(game.getQuarterScores()).hasSize(4);
        assertThatThrownBy(() -> game.getQuarterScores().clear())
                .isInstanceOf(UnsupportedOperationException.class);

        game.recordQuarters(List.of());
        assertThat(game.getQuarterScores()).isEmpty();
    }

    @Test
    void rejectsRecordingQuartersOutsideLiveStatus() {
        final Game scheduled = new Game(SEASON_ID, 1, HOME, AWAY);
        final Game finished = finishedGame();
        final Game confirmed = finishedGame();
        confirmed.recordPlayerPoints(matchingPlayerPoints());
        confirmed.confirm(0L);
        final QuarterScore q1 = new QuarterScore(1, 20, 18);

        assertThatThrownBy(() -> scheduled.recordQuarter(q1))
                .isInstanceOf(InvalidGameStatusTransitionException.class);
        assertThatThrownBy(() -> scheduled.recordQuarters(List.of(q1)))
                .isInstanceOf(InvalidGameStatusTransitionException.class);
        assertThatThrownBy(() -> finished.recordQuarter(q1))
                .isInstanceOf(InvalidGameStatusTransitionException.class);
        assertThatThrownBy(() -> finished.recordQuarters(fourRegulationQuarters()))
                .isInstanceOf(InvalidGameStatusTransitionException.class);
        assertThatThrownBy(() -> confirmed.recordQuarters(fourRegulationQuarters()))
                .isInstanceOf(InvalidGameStatusTransitionException.class);
    }

    @Test
    void rejectsNullQuarterInputsAndKeepsExistingQuarters() {
        final Game game = liveGame();
        final QuarterScore q1 = new QuarterScore(1, 20, 18);
        game.recordQuarter(q1);

        final List<QuarterScore> withNullElement = new ArrayList<>();
        withNullElement.add(q1);
        withNullElement.add(null);

        assertThatThrownBy(() -> game.recordQuarter(null))
                .isInstanceOf(InvalidQuarterScoreException.class)
                .extracting("code").isEqualTo(ErrorCode.INVALID_QUARTER_SCORE);
        assertThatThrownBy(() -> game.recordQuarters(null))
                .isInstanceOf(InvalidQuarterScoreException.class);
        assertThatThrownBy(() -> game.recordQuarters(withNullElement))
                .isInstanceOf(InvalidQuarterScoreException.class);
        assertThat(game.getQuarterScores()).containsExactly(q1);
    }

    @Test
    void rejectsOutOfOrderGapsAndDuplicateQuarterNumbers() {
        final Game game = liveGame();

        assertThatThrownBy(() -> game.recordQuarter(new QuarterScore(2, 20, 18)))
                .isInstanceOf(InvalidQuarterScoreException.class);
        game.recordQuarter(new QuarterScore(1, 20, 18));
        assertThatThrownBy(() -> game.recordQuarter(new QuarterScore(3, 20, 18)))
                .isInstanceOf(InvalidQuarterScoreException.class);

        assertThatThrownBy(() -> game.recordQuarters(List.of(
                new QuarterScore(2, 20, 18),
                new QuarterScore(1, 18, 20))))
                .isInstanceOf(InvalidQuarterScoreException.class);
        assertThatThrownBy(() -> game.recordQuarters(List.of(
                new QuarterScore(1, 20, 18),
                new QuarterScore(3, 18, 20))))
                .isInstanceOf(InvalidQuarterScoreException.class);
        assertThatThrownBy(() -> game.recordQuarters(List.of(
                new QuarterScore(1, 20, 18),
                new QuarterScore(1, 18, 20))))
                .isInstanceOf(InvalidQuarterScoreException.class);

        assertThat(game.getQuarterScores()).containsExactly(new QuarterScore(1, 20, 18));
    }

    @Test
    void rejectsOvertimeWhenRegulationIsNotTied() {
        final Game game = liveGame();
        game.recordQuarters(fourRegulationQuarters()); // 85-76

        assertThatThrownBy(() -> game.recordQuarter(new QuarterScore(5, 8, 6)))
                .isInstanceOf(InvalidQuarterScoreException.class)
                .extracting("code").isEqualTo(ErrorCode.INVALID_QUARTER_SCORE);
        assertThat(game.getQuarterScores()).hasSize(4);

        final List<QuarterScore> withUnneededOt = new ArrayList<>(fourRegulationQuarters());
        withUnneededOt.add(new QuarterScore(5, 8, 6));
        assertThatThrownBy(() -> game.recordQuarters(withUnneededOt))
                .isInstanceOf(InvalidQuarterScoreException.class);
    }

    @Test
    void allowsOvertimeOnlyWhilePreviousPeriodEndedTiedAndRejectsExtraOtAfterTieBroken() {
        final Game game = liveGame();
        game.recordQuarters(tiedRegulationQuarters()); // 80-80

        game.recordQuarter(new QuarterScore(5, 9, 9)); // 89-89
        game.recordQuarter(new QuarterScore(6, 11, 7)); // 100-96

        assertThat(game.getQuarterScores()).hasSize(6);

        assertThatThrownBy(() -> game.recordQuarter(new QuarterScore(7, 5, 3)))
                .isInstanceOf(InvalidQuarterScoreException.class);

        // Editing Q1 to break the regulation tie invalidates existing OT quarters and rolls back in memory
        assertThatThrownBy(() -> game.recordQuarter(new QuarterScore(1, 21, 18)))
                .isInstanceOf(InvalidQuarterScoreException.class);
        assertThat(game.getQuarterScores()).hasSize(6);
    }

    @Test
    void overtimeValidationRejectsOverflowInsteadOfWrappingRegulationScoreToZero() {
        final Game game = liveGame();
        game.recordQuarters(List.of(
                new QuarterScore(1, Integer.MAX_VALUE, 0),
                new QuarterScore(2, Integer.MAX_VALUE, 0),
                new QuarterScore(3, 2, 0),
                new QuarterScore(4, 0, 0)));

        assertThatThrownBy(() -> game.recordQuarter(new QuarterScore(5, 5, 3)))
                .isInstanceOf(ArithmeticException.class);
        assertThat(game.getQuarterScores()).hasSize(4);
    }

    @Test
    void finishCalculatesTotalsAndMovesToFinished() {
        final Game game = liveGame();
        game.recordQuarters(fourRegulationQuarters());

        game.finish();

        assertThat(game.getStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(game.getHomeScore()).isEqualTo(85);
        assertThat(game.getAwayScore()).isEqualTo(76);
    }

    @Test
    void finishWorksWithOvertimeAndZeroPointsSide() {
        final Game otGame = liveGame();
        otGame.recordQuarters(tiedRegulationQuarters());
        otGame.recordQuarter(new QuarterScore(5, 10, 6));

        otGame.finish();

        assertThat(otGame.getStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(otGame.getHomeScore()).isEqualTo(90);
        assertThat(otGame.getAwayScore()).isEqualTo(86);

        final Game minimalWin = liveGame();
        minimalWin.recordQuarters(List.of(
                new QuarterScore(1, 1, 0),
                new QuarterScore(2, 0, 0),
                new QuarterScore(3, 0, 0),
                new QuarterScore(4, 0, 0)));

        minimalWin.finish();

        assertThat(minimalWin.getHomeScore()).isEqualTo(1);
        assertThat(minimalWin.getAwayScore()).isZero();
    }

    @Test
    void finishRejectsFewerThanFourQuartersWithoutChangingStatusOrScores() {
        final Game game = liveGame();

        for (int q = 1; q <= 3; q++) {
            assertThatThrownBy(game::finish)
                    .isInstanceOf(InvalidQuarterScoreException.class)
                    .extracting("code").isEqualTo(ErrorCode.INVALID_QUARTER_SCORE);
            game.recordQuarter(new QuarterScore(q, 20, 18));
        }

        assertThatThrownBy(game::finish)
                .isInstanceOf(InvalidQuarterScoreException.class);
        assertThat(game.getStatus()).isEqualTo(GameStatus.LIVE);
        assertThat(game.getHomeScore()).isNull();
        assertThat(game.getAwayScore()).isNull();
    }

    @Test
    void finishRejectsTiedGameAfterRegulationOrOvertimeAndRequiresOvertime() {
        final Game regTied = liveGame();
        regTied.recordQuarters(tiedRegulationQuarters());

        assertThatThrownBy(regTied::finish)
                .isInstanceOf(OvertimeRequiredException.class)
                .extracting("code").isEqualTo(ErrorCode.OVERTIME_REQUIRED);
        assertThat(regTied.getStatus()).isEqualTo(GameStatus.LIVE);
        assertThat(regTied.getHomeScore()).isNull();

        regTied.recordQuarter(new QuarterScore(5, 10, 10));
        assertThatThrownBy(regTied::finish)
                .isInstanceOf(OvertimeRequiredException.class)
                .extracting("code").isEqualTo(ErrorCode.OVERTIME_REQUIRED);
    }

    @Test
    void finishFailsOnIntegerOverflowInsteadOfWrappingAround() {
        final Game homeOverflow = liveGame();
        homeOverflow.recordQuarters(List.of(
                new QuarterScore(1, Integer.MAX_VALUE, 0),
                new QuarterScore(2, 1, 0),
                new QuarterScore(3, 0, 0),
                new QuarterScore(4, 0, 10)));

        assertThatThrownBy(homeOverflow::finish).isInstanceOf(ArithmeticException.class);
        assertThat(homeOverflow.getStatus()).isEqualTo(GameStatus.LIVE);
        assertThat(homeOverflow.getHomeScore()).isNull();

        final Game awayOverflow = liveGame();
        awayOverflow.recordQuarters(List.of(
                new QuarterScore(1, 0, Integer.MAX_VALUE),
                new QuarterScore(2, 0, 1),
                new QuarterScore(3, 0, 0),
                new QuarterScore(4, 10, 0)));

        assertThatThrownBy(awayOverflow::finish).isInstanceOf(ArithmeticException.class);
        assertThat(awayOverflow.getAwayScore()).isNull();
    }

    @Test
    void finishRejectsNonLiveStatus() {
        final Game scheduled = new Game(SEASON_ID, 1, HOME, AWAY);
        final Game finished = finishedGame();

        assertThatThrownBy(scheduled::finish).isInstanceOf(InvalidGameStatusTransitionException.class);
        assertThatThrownBy(finished::finish).isInstanceOf(InvalidGameStatusTransitionException.class);
    }

    @Test
    void reopenMovesFinishedGameToLiveAndClearsStoredScoresForCorrection() {
        final Game game = finishedGame();

        game.reopen();

        assertThat(game.getStatus()).isEqualTo(GameStatus.LIVE);
        assertThat(game.getHomeScore()).isNull();
        assertThat(game.getAwayScore()).isNull();
        assertThat(game.getQuarterScores()).hasSize(4);

        game.recordQuarter(new QuarterScore(4, 30, 20));
        game.finish();

        assertThat(game.getStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(game.getHomeScore()).isEqualTo(91);
        assertThat(game.getAwayScore()).isEqualTo(76);
    }

    @Test
    void reopenRejectsScheduledLiveAndConfirmedGames() {
        final Game scheduled = new Game(SEASON_ID, 1, HOME, AWAY);
        final Game live = liveGame();
        final Game confirmed = finishedGame();
        confirmed.recordPlayerPoints(matchingPlayerPoints());
        confirmed.confirm(0L);

        assertThatThrownBy(scheduled::reopen).isInstanceOf(InvalidGameStatusTransitionException.class);
        assertThatThrownBy(live::reopen).isInstanceOf(InvalidGameStatusTransitionException.class);
        assertThatThrownBy(confirmed::reopen).isInstanceOf(InvalidGameStatusTransitionException.class);
    }

    @Test
    void recordsPlayerPointsWhileLiveOrFinishedAndReplacesListWholesale() {
        final Game game = liveGame();
        final List<PlayerPoints> initial = new ArrayList<>(List.of(
                new PlayerPoints(101L, HOME, 12),
                new PlayerPoints(201L, AWAY, 0)));

        game.recordPlayerPoints(initial);
        initial.clear();

        assertThat(game.getPlayerPoints()).containsExactly(
                new PlayerPoints(101L, HOME, 12),
                new PlayerPoints(201L, AWAY, 0));
        assertThatThrownBy(() -> game.getPlayerPoints().clear())
                .isInstanceOf(UnsupportedOperationException.class);

        game.recordQuarters(fourRegulationQuarters());
        game.finish();
        game.recordPlayerPoints(matchingPlayerPoints());

        assertThat(game.getPlayerPoints()).hasSize(5);
    }

    @Test
    void rejectsInvalidPlayerPointsEntriesAndNonParticipantTeams() {
        final Game game = liveGame();
        final PlayerPoints existing = new PlayerPoints(101L, HOME, 10);
        game.recordPlayerPoints(List.of(existing));

        final List<PlayerPoints> withNull = new ArrayList<>();
        withNull.add(null);

        assertThatThrownBy(() -> game.recordPlayerPoints(null))
                .isInstanceOf(InvalidPlayerPointsException.class);
        assertThatThrownBy(() -> game.recordPlayerPoints(withNull))
                .isInstanceOf(InvalidPlayerPointsException.class);
        assertThatThrownBy(() -> game.recordPlayerPoints(List.of(new PlayerPoints(301L, OTHER_TEAM, 15))))
                .isInstanceOf(PlayerNotInGameException.class)
                .extracting("code").isEqualTo(ErrorCode.PLAYER_NOT_IN_GAME);
        assertThatThrownBy(() -> game.recordPlayerPoints(List.of(
                new PlayerPoints(101L, HOME, 10),
                new PlayerPoints(101L, HOME, 20))))
                .isInstanceOf(InvalidPlayerPointsException.class);

        assertThat(game.getPlayerPoints()).containsExactly(existing);
    }

    @Test
    void rejectsRecordingPlayerPointsWhenScheduledOrConfirmed() {
        final Game scheduled = new Game(SEASON_ID, 1, HOME, AWAY);
        final Game confirmed = finishedGame();
        confirmed.recordPlayerPoints(matchingPlayerPoints());
        confirmed.confirm(0L);

        assertThatThrownBy(() -> scheduled.recordPlayerPoints(matchingPlayerPoints()))
                .isInstanceOf(InvalidGameStatusTransitionException.class);
        assertThatThrownBy(() -> confirmed.recordPlayerPoints(matchingPlayerPoints()))
                .isInstanceOf(InvalidGameStatusTransitionException.class);
    }

    @Test
    void confirmSucceedsWhenPlayerPointsMatchTeamScoresAndLocksGame() {
        final Game game = withVersion(finishedGame(), 7L);
        game.recordPlayerPoints(matchingPlayerPoints());

        game.confirm(7L);

        assertThat(game.getStatus()).isEqualTo(GameStatus.CONFIRMED);
        assertThatThrownBy(game::start).isInstanceOf(InvalidGameStatusTransitionException.class);
        assertThatThrownBy(() -> game.recordQuarter(new QuarterScore(1, 20, 18)))
                .isInstanceOf(InvalidGameStatusTransitionException.class);
        assertThatThrownBy(() -> game.recordQuarters(fourRegulationQuarters()))
                .isInstanceOf(InvalidGameStatusTransitionException.class);
        assertThatThrownBy(() -> game.recordPlayerPoints(matchingPlayerPoints()))
                .isInstanceOf(InvalidGameStatusTransitionException.class);
        assertThatThrownBy(game::finish).isInstanceOf(InvalidGameStatusTransitionException.class);
        assertThatThrownBy(game::reopen).isInstanceOf(InvalidGameStatusTransitionException.class);
        assertThatThrownBy(() -> game.confirm(7L)).isInstanceOf(InvalidGameStatusTransitionException.class);
    }

    @Test
    void confirmAllowsTeamWithZeroScoreWhenItHasNoOrZeroPlayerStats() {
        final Game game = liveGame();
        game.recordQuarters(List.of(
                new QuarterScore(1, 2, 0),
                new QuarterScore(2, 0, 0),
                new QuarterScore(3, 0, 0),
                new QuarterScore(4, 0, 0)));
        game.finish();
        game.recordPlayerPoints(List.of(new PlayerPoints(101L, HOME, 2)));

        game.confirm(0L);

        assertThat(game.getStatus()).isEqualTo(GameStatus.CONFIRMED);
    }

    @Test
    void confirmRejectsFinishedGameWhoseQuarterScoresAreMissing() {
        final Game game = finishedGame();
        game.recordPlayerPoints(matchingPlayerPoints());
        withField(game, "quarterScores", new ArrayList<QuarterScore>());

        assertThatThrownBy(() -> game.confirm(0L))
                .isInstanceOf(InvalidQuarterScoreException.class)
                .extracting("code").isEqualTo(ErrorCode.INVALID_QUARTER_SCORE);
        assertThat(game.getStatus()).isEqualTo(GameStatus.FINISHED);
    }

    @Test
    void confirmRejectsStaleVersionWithoutConfirming() {
        final Game game = withVersion(finishedGame(), 7L);
        game.recordPlayerPoints(matchingPlayerPoints());

        assertThatThrownBy(() -> game.confirm(6L))
                .isInstanceOf(ConcurrentUpdateException.class)
                .extracting("code").isEqualTo(ErrorCode.CONCURRENT_UPDATE);
        assertThatThrownBy(() -> game.confirm(8L))
                .isInstanceOf(ConcurrentUpdateException.class);
        assertThat(game.getStatus()).isEqualTo(GameStatus.FINISHED);
    }

    @Test
    void confirmRejectsPlayerPointsMismatchOnEitherSide() {
        final Game game = finishedGame(); // 85-76

        assertThatThrownBy(() -> game.confirm(0L))
                .isInstanceOf(ScoreMismatchException.class)
                .extracting("code").isEqualTo(ErrorCode.SCORE_MISMATCH);

        game.recordPlayerPoints(List.of(
                new PlayerPoints(101L, HOME, 84),
                new PlayerPoints(201L, AWAY, 76)));
        assertThatThrownBy(() -> game.confirm(0L))
                .isInstanceOf(ScoreMismatchException.class)
                .extracting("code").isEqualTo(ErrorCode.SCORE_MISMATCH);

        game.recordPlayerPoints(List.of(
                new PlayerPoints(101L, HOME, 85),
                new PlayerPoints(201L, AWAY, 77)));
        assertThatThrownBy(() -> game.confirm(0L))
                .isInstanceOf(ScoreMismatchException.class)
                .extracting("code").isEqualTo(ErrorCode.SCORE_MISMATCH);

        assertThat(game.getStatus()).isEqualTo(GameStatus.FINISHED);
    }

    @Test
    void confirmFailsOnPlayerPointsIntegerOverflowInsteadOfWrappingToScore() {
        final Game game = finishedGame(); // 85-76
        game.recordPlayerPoints(List.of(
                new PlayerPoints(101L, HOME, Integer.MAX_VALUE),
                new PlayerPoints(102L, HOME, Integer.MAX_VALUE),
                new PlayerPoints(103L, HOME, 87),
                new PlayerPoints(201L, AWAY, 76)));

        assertThatThrownBy(() -> game.confirm(0L)).isInstanceOf(ArithmeticException.class);
        assertThat(game.getStatus()).isEqualTo(GameStatus.FINISHED);
    }

    @Test
    void confirmRejectsScheduledOrLiveGame() {
        final Game scheduled = new Game(SEASON_ID, 1, HOME, AWAY);
        final Game live = liveGame();

        assertThatThrownBy(() -> scheduled.confirm(0L))
                .isInstanceOf(InvalidGameStatusTransitionException.class)
                .extracting("code").isEqualTo(ErrorCode.INVALID_GAME_STATUS_TRANSITION);
        assertThatThrownBy(() -> live.confirm(0L))
                .isInstanceOf(InvalidGameStatusTransitionException.class);
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

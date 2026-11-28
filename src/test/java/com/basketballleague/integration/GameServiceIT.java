package com.basketballleague.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.basketballleague.domain.ConcurrentUpdateException;
import com.basketballleague.domain.GameStatus;
import com.basketballleague.domain.ScoreMismatchException;
import com.basketballleague.dto.GameDetailResponse;
import com.basketballleague.dto.PlayerPointsRequest;
import com.basketballleague.dto.QuarterScoreRequest;
import com.basketballleague.dto.RecordPlayerPointsRequest;
import com.basketballleague.dto.RecordQuartersRequest;
import com.basketballleague.entity.Game;
import com.basketballleague.entity.PlayerPoints;
import com.basketballleague.entity.QuarterScore;
import com.basketballleague.entity.Season;
import com.basketballleague.entity.Team;
import com.basketballleague.repository.GameRepository;
import com.basketballleague.repository.PlayerRepository;
import com.basketballleague.repository.SeasonRepository;
import com.basketballleague.repository.TeamRepository;
import com.basketballleague.service.GameService;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Result entry and confirmation against PostgreSQL 18 (ADR-0004, "Doğrulama"). Each service call runs inside a
 * RESOURCE_LOCAL transaction from {@link PostgresIntegrationSupport}, which stands in for the {@code @Transactional}
 * boundary that JTA provides on Payara: a runtime exception rolls the whole call back in both.
 */
class GameServiceIT extends PostgresIntegrationSupport {

    private static final int QUARTER_HOME_POINTS = 20;
    private static final int QUARTER_AWAY_POINTS = 18;
    private static final int HOME_TOTAL = 80;
    private static final int AWAY_TOTAL = 72;
    private static final int REGULATION_QUARTERS = 4;

    private long seasonId;
    private long homePlayerId;
    private long awayPlayerId;

    @BeforeEach
    void resetDatabase() {
        resetToSeedState();
    }

    @Test
    void failedConfirmLeavesGameFinishedAndOutOfStandings() {
        final long gameId = finishedGameWithAwayPoints(AWAY_TOTAL - 2);
        final long versionBefore = storedGame(gameId).getVersion();

        assertThatThrownBy(() -> inTransaction(em -> service(em).confirm(gameId, versionBefore)))
                .isInstanceOf(ScoreMismatchException.class);

        final Game stored = storedGame(gameId);
        assertThat(stored.getStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(stored.getVersion()).isEqualTo(versionBefore);
        assertThat(confirmedGameIds()).doesNotContain(gameId);
    }

    @Test
    void confirmWithMatchingPointsCountsInStandings() {
        final long gameId = finishedGameWithAwayPoints(AWAY_TOTAL);
        final long versionBefore = storedGame(gameId).getVersion();

        final GameDetailResponse confirmed = inTransaction(em -> service(em).confirm(gameId, versionBefore));

        assertThat(confirmed.status()).isEqualTo("CONFIRMED");
        assertThat(storedGame(gameId).getStatus()).isEqualTo(GameStatus.CONFIRMED);
        assertThat(confirmedGameIds()).containsExactly(gameId);
    }

    @Test
    void onlyPlayerPointsChangeIncrementsVersionOnce() {
        final long gameId = finishedGameWithAwayPoints(AWAY_TOTAL - 2);
        final long versionBefore = storedGame(gameId).getVersion();

        final GameDetailResponse response = inTransaction(em -> service(em).recordPlayerPoints(gameId,
                new RecordPlayerPointsRequest(versionBefore, List.of(
                        new PlayerPointsRequest(homePlayerId, HOME_TOTAL),
                        new PlayerPointsRequest(awayPlayerId, AWAY_TOTAL)))));

        assertThat(storedGame(gameId).getVersion()).isEqualTo(versionBefore + 1);
        assertThat(response.version()).isEqualTo(versionBefore + 1);
        assertThat(storedGame(gameId).getPlayerPoints())
                .extracting(PlayerPoints::getPoints).containsExactlyInAnyOrder(HOME_TOTAL, AWAY_TOTAL);
    }

    @Test
    void statusChangeIncrementsVersionOnce() {
        final long gameId = scheduledGame();
        final long versionBefore = storedGame(gameId).getVersion();

        inTransaction(em -> service(em).start(gameId, versionBefore));

        assertThat(storedGame(gameId).getStatus()).isEqualTo(GameStatus.LIVE);
        assertThat(storedGame(gameId).getVersion()).isEqualTo(versionBefore + 1);
    }

    @Test
    void responseVersionMatchesStoredVersionAfterEachChange() {
        final long gameId = scheduledGame();

        final GameDetailResponse started =
                inTransaction(em -> service(em).start(gameId, storedGame(gameId).getVersion()));
        assertThat(started.version()).isEqualTo(storedGame(gameId).getVersion());

        final GameDetailResponse withQuarters = inTransaction(em -> service(em).recordQuarters(gameId,
                new RecordQuartersRequest(started.version(), regulationQuarterRequests())));
        assertThat(withQuarters.version()).isEqualTo(storedGame(gameId).getVersion());

        final GameDetailResponse finished =
                inTransaction(em -> service(em).finish(gameId, withQuarters.version()));
        assertThat(finished.version()).isEqualTo(storedGame(gameId).getVersion());
        assertThat(finished.homeScore()).isEqualTo(HOME_TOTAL);
        assertThat(finished.awayScore()).isEqualTo(AWAY_TOTAL);
    }

    @Test
    void staleVersionFromASecondScreenIsRejected() {
        final long gameId = scheduledGame();
        final long live = inTransaction(em -> service(em).start(gameId, storedGame(gameId).getVersion())).version();
        final List<QuarterScoreRequest> firstEntry = List.of(new QuarterScoreRequest(1, 22, 18));
        final List<QuarterScoreRequest> secondEntry = List.of(new QuarterScoreRequest(1, 10, 10));

        inTransaction(em -> service(em).recordQuarters(gameId, new RecordQuartersRequest(live, firstEntry)));

        assertThatThrownBy(() -> inTransaction(em ->
                service(em).recordQuarters(gameId, new RecordQuartersRequest(live, secondEntry))))
                .isInstanceOf(ConcurrentUpdateException.class);
        assertThat(storedGame(gameId).getQuarterScores()).containsExactly(new QuarterScore(1, 22, 18));
    }

    private static GameService service(EntityManager em) {
        return new GameService(new GameRepository(em), new PlayerRepository(em), new TeamRepository(em));
    }

    private static Game storedGame(long gameId) {
        return readOnly(em -> {
            final Game game = em.find(Game.class, gameId);
            game.getQuarterScores().size();
            game.getPlayerPoints().size();
            return game;
        });
    }

    private List<Long> confirmedGameIds() {
        return readOnly(em -> new GameRepository(em).findConfirmedBySeasonId(seasonId).stream()
                .map(Game::getId).toList());
    }

    /** A new season with one game between two seeded teams; also picks one seeded player of each team. */
    private long scheduledGame() {
        return inTransaction(em -> {
            final List<Team> teams = new TeamRepository(em).findPage(0, 2);
            final PlayerRepository players = new PlayerRepository(em);
            homePlayerId = players.findByTeamId(teams.get(0).getId()).getFirst().getId();
            awayPlayerId = players.findByTeamId(teams.get(1).getId()).getFirst().getId();
            final Season season =
                    new SeasonRepository(em).save(new Season("Onay Sezonu", LocalDate.of(2026, 10, 1)));
            seasonId = season.getId();
            final Game game = new Game(seasonId, 1, teams.get(0).getId(), teams.get(1).getId());
            new GameRepository(em).saveAll(List.of(game));
            return game.getId();
        });
    }

    /** A finished 80-72 game whose away player points add up to {@code awayPoints}. */
    private long finishedGameWithAwayPoints(int awayPoints) {
        final long gameId = scheduledGame();
        inTransactionVoid(em -> {
            final Game game = em.find(Game.class, gameId);
            game.start();
            game.recordQuarters(regulationQuarters());
            game.finish();
            game.recordPlayerPoints(List.of(
                    new PlayerPoints(homePlayerId, game.getHomeTeamId(), HOME_TOTAL),
                    new PlayerPoints(awayPlayerId, game.getAwayTeamId(), awayPoints)));
        });
        return gameId;
    }

    private static List<QuarterScore> regulationQuarters() {
        return regulationQuarterRequests().stream()
                .map(q -> new QuarterScore(q.quarterNo(), q.homePoints(), q.awayPoints())).toList();
    }

    private static List<QuarterScoreRequest> regulationQuarterRequests() {
        return IntStream.rangeClosed(1, REGULATION_QUARTERS)
                .mapToObj(number -> new QuarterScoreRequest(number, QUARTER_HOME_POINTS, QUARTER_AWAY_POINTS))
                .toList();
    }
}

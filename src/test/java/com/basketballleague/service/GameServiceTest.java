package com.basketballleague.service;

import static com.basketballleague.support.EntityFields.withField;
import static com.basketballleague.support.EntityIds.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.basketballleague.domain.ConcurrentUpdateException;
import com.basketballleague.domain.ErrorCode;
import com.basketballleague.domain.GameNotFoundException;
import com.basketballleague.domain.GameStatus;
import com.basketballleague.domain.InvalidQuarterScoreException;
import com.basketballleague.domain.PlayerNotInGameException;
import com.basketballleague.dto.GameDetailResponse;
import com.basketballleague.dto.PlayerPointsRequest;
import com.basketballleague.dto.PlayerPointsResponse;
import com.basketballleague.dto.QuarterScoreRequest;
import com.basketballleague.dto.RecordPlayerPointsRequest;
import com.basketballleague.dto.RecordQuartersRequest;
import com.basketballleague.entity.Game;
import com.basketballleague.entity.Player;
import com.basketballleague.entity.PlayerPoints;
import com.basketballleague.entity.Position;
import com.basketballleague.entity.QuarterScore;
import com.basketballleague.entity.Team;
import com.basketballleague.repository.GameRepository;
import com.basketballleague.repository.PlayerRepository;
import com.basketballleague.repository.TeamRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    private static final long GAME_ID = 10L;
    private static final long VERSION = 4L;
    private static final Team HOME = withId(new Team("Anadolu Efes", "İstanbul"), 1L);
    private static final Team AWAY = withId(new Team("Fenerbahçe Beko", "İstanbul"), 9L);

    @Mock
    private GameRepository gameRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private TeamRepository teamRepository;

    private GameService service() {
        return new GameService(gameRepository, playerRepository, teamRepository);
    }

    @BeforeEach
    void teamsAreFound() {
        lenient().when(teamRepository.findByIds(anyCollection())).thenReturn(List.of(HOME, AWAY));
    }

    private Game givenGame(GameStatus target) {
        final Game game = withId(new Game(3L, 1, HOME.getId(), AWAY.getId()), GAME_ID);
        if (target != GameStatus.SCHEDULED) {
            game.start();
        }
        if (target == GameStatus.FINISHED) {
            game.recordQuarters(List.of(new QuarterScore(1, 20, 18), new QuarterScore(2, 20, 18),
                    new QuarterScore(3, 20, 18), new QuarterScore(4, 20, 18)));
            game.finish();
        }
        withField(game, "version", VERSION);
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        return game;
    }

    @Test
    void getGameThrowsNotFoundForAnUnknownId() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getGame(GAME_ID)).isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void getGameReturnsTheGameWithBothTeams() {
        givenGame(GameStatus.SCHEDULED);

        final GameDetailResponse response = service().getGame(GAME_ID);

        assertThat(response.homeTeam().name()).isEqualTo("Anadolu Efes");
        assertThat(response.awayTeam().name()).isEqualTo("Fenerbahçe Beko");
        verify(gameRepository, never()).forceVersionIncrement(any());
    }

    @Test
    void staleVersionChangesNothing() {
        final Game game = givenGame(GameStatus.SCHEDULED);

        assertThatThrownBy(() -> service().start(GAME_ID, VERSION - 1))
                .isInstanceOf(ConcurrentUpdateException.class)
                .extracting("code").isEqualTo(ErrorCode.CONCURRENT_UPDATE);
        assertThat(game.getStatus()).isEqualTo(GameStatus.SCHEDULED);
        verify(gameRepository, never()).forceVersionIncrement(any());
    }

    @Test
    void startMovesTheGameToLiveAndIncrementsItsVersion() {
        final Game game = givenGame(GameStatus.SCHEDULED);

        final GameDetailResponse response = service().start(GAME_ID, VERSION);

        assertThat(response.status()).isEqualTo("LIVE");
        verify(gameRepository).forceVersionIncrement(game);
    }

    @Test
    void domainRuleViolationDoesNotIncrementTheVersion() {
        final Game game = givenGame(GameStatus.LIVE);
        game.recordQuarters(List.of(new QuarterScore(1, 20, 18)));

        assertThatThrownBy(() -> service().finish(GAME_ID, VERSION)).isInstanceOf(InvalidQuarterScoreException.class);
        verify(gameRepository, never()).forceVersionIncrement(any());
    }

    @Test
    void recordQuartersReplacesTheQuarterList() {
        givenGame(GameStatus.LIVE);

        final GameDetailResponse response = service().recordQuarters(GAME_ID,
                new RecordQuartersRequest(VERSION, List.of(new QuarterScoreRequest(1, 22, 18))));

        assertThat(response.quarters()).singleElement().satisfies(q -> assertThat(q.homePoints()).isEqualTo(22));
    }

    @Test
    void finishAndReopenFollowTheGameRules() {
        final Game game = givenGame(GameStatus.FINISHED);

        final GameDetailResponse reopened = service().reopen(GAME_ID, VERSION);

        assertThat(reopened.status()).isEqualTo("LIVE");
        assertThat(reopened.homeScore()).isNull();
        withField(game, "version", VERSION);
        assertThat(service().finish(GAME_ID, VERSION).homeScore()).isEqualTo(80);
        verify(gameRepository, times(2)).forceVersionIncrement(game);
    }

    @Test
    void recordPlayerPointsUsesEachPlayersCurrentTeam() {
        givenGame(GameStatus.FINISHED);
        final Player shooter = withId(new Player(HOME, "Shane", "Larkin", 3, Position.PG), 7L);
        when(playerRepository.findByIds(List.of(7L))).thenReturn(List.of(shooter));

        final GameDetailResponse response = service().recordPlayerPoints(GAME_ID,
                new RecordPlayerPointsRequest(VERSION, List.of(new PlayerPointsRequest(7L, 21))));

        assertThat(response.playerPoints()).containsExactly(new PlayerPointsResponse(7L, HOME.getId(), 21));
        verify(playerRepository, times(1)).findByIds(any());
        verify(playerRepository, never()).findById(any());
    }

    @Test
    void recordPlayerPointsRejectsAnUnknownPlayer() {
        final Game game = givenGame(GameStatus.FINISHED);
        when(playerRepository.findByIds(List.of(999L))).thenReturn(List.of());

        assertThatThrownBy(() -> service().recordPlayerPoints(GAME_ID,
                new RecordPlayerPointsRequest(VERSION, List.of(new PlayerPointsRequest(999L, 5)))))
                .isInstanceOf(PlayerNotInGameException.class)
                .extracting("code").isEqualTo(ErrorCode.PLAYER_NOT_IN_GAME);
        assertThat(game.getPlayerPoints()).isEmpty();
        verify(gameRepository, never()).forceVersionIncrement(any());
    }

    @Test
    void confirmLocksAFinishedGameWhosePointsMatch() {
        final Game game = givenGame(GameStatus.FINISHED);
        game.recordPlayerPoints(List.of(
                new PlayerPoints(7L, HOME.getId(), 80), new PlayerPoints(40L, AWAY.getId(), 72)));

        assertThat(service().confirm(GAME_ID, VERSION).status()).isEqualTo("CONFIRMED");
        verify(gameRepository).forceVersionIncrement(game);
    }

    @Test
    void confirmWithAStaleVersionIsRejected() {
        givenGame(GameStatus.FINISHED);

        assertThatThrownBy(() -> service().confirm(GAME_ID, VERSION + 1))
                .isInstanceOf(ConcurrentUpdateException.class);
        verify(gameRepository, never()).forceVersionIncrement(any());
    }
}

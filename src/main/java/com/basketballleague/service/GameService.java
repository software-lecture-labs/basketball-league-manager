package com.basketballleague.service;

import com.basketballleague.domain.ConcurrentUpdateException;
import com.basketballleague.domain.GameNotFoundException;
import com.basketballleague.domain.PlayerNotInGameException;
import com.basketballleague.dto.GameDetailResponse;
import com.basketballleague.dto.PlayerPointsRequest;
import com.basketballleague.dto.RecordPlayerPointsRequest;
import com.basketballleague.dto.RecordQuartersRequest;
import com.basketballleague.dto.TeamResponse;
import com.basketballleague.entity.Game;
import com.basketballleague.entity.Player;
import com.basketballleague.entity.PlayerPoints;
import com.basketballleague.entity.Team;
import com.basketballleague.repository.GameRepository;
import com.basketballleague.repository.PlayerRepository;
import com.basketballleague.repository.TeamRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Result entry and confirmation of a game (ADR-0004). The rules live in {@link Game}; this class loads the game,
 * compares the version the client last read, applies one change and raises the version, all in one transaction.
 * A rule violation is a runtime exception, so JTA rolls the whole change back; nothing is undone by hand here.
 */
@ApplicationScoped
public class GameService {

    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;

    /** Required by CDI to create the proxy; never used to serve calls. */
    protected GameService() {
        this.gameRepository = null;
        this.playerRepository = null;
        this.teamRepository = null;
    }

    @Inject
    public GameService(GameRepository gameRepository, PlayerRepository playerRepository,
            TeamRepository teamRepository) {
        this.gameRepository = gameRepository;
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
    }

    public GameDetailResponse getGame(long gameId) {
        return toResponse(load(gameId));
    }

    @Transactional
    public GameDetailResponse start(long gameId, long expectedVersion) {
        return change(gameId, expectedVersion, Game::start);
    }

    @Transactional
    public GameDetailResponse recordQuarters(long gameId, RecordQuartersRequest request) {
        return change(gameId, request.version(),
                game -> game.recordQuarters(GameMapper.toQuarterScores(request.quarters())));
    }

    /**
     * The players are loaded in one query; each entry takes the player's current team, so a later transfer does
     * not move points recorded for this game (ADR-0004).
     */
    @Transactional
    public GameDetailResponse recordPlayerPoints(long gameId, RecordPlayerPointsRequest request) {
        return change(gameId, request.version(), game -> game.recordPlayerPoints(toPlayerPoints(game, request)));
    }

    @Transactional
    public GameDetailResponse finish(long gameId, long expectedVersion) {
        return change(gameId, expectedVersion, Game::finish);
    }

    @Transactional
    public GameDetailResponse reopen(long gameId, long expectedVersion) {
        return change(gameId, expectedVersion, Game::reopen);
    }

    /**
     * The main transaction of the project: if any check fails, the game stays FINISHED and the standings, which
     * are computed from confirmed games, do not count it (ADR-0001, ADR-0004).
     */
    @Transactional
    public GameDetailResponse confirm(long gameId, long expectedVersion) {
        return change(gameId, expectedVersion, game -> game.confirm(expectedVersion));
    }

    /**
     * The order matters: the version is compared before anything changes, and raised only after the domain change
     * succeeded, because JPA may apply a forced increment at once.
     */
    private GameDetailResponse change(long gameId, long expectedVersion, Consumer<Game> action) {
        final Game game = load(gameId);
        if (game.getVersion() != expectedVersion) {
            throw new ConcurrentUpdateException(game.getId(), expectedVersion, game.getVersion());
        }
        action.accept(game);
        gameRepository.forceVersionIncrement(game);
        return toResponse(game);
    }

    private Game load(long gameId) {
        return gameRepository.findById(gameId).orElseThrow(() -> new GameNotFoundException(gameId));
    }

    private List<PlayerPoints> toPlayerPoints(Game game, RecordPlayerPointsRequest request) {
        final List<Long> playerIds = request.players().stream().map(PlayerPointsRequest::playerId).toList();
        final Map<Long, Player> playersById = playerRepository.findByIds(playerIds).stream()
                .collect(Collectors.toMap(Player::getId, Function.identity()));
        return request.players().stream().map(entry -> {
            final Player player = playersById.get(entry.playerId());
            if (player == null) {
                throw new PlayerNotInGameException(entry.playerId(), game.getId());
            }
            return new PlayerPoints(entry.playerId(), player.getTeam().getId(), entry.points());
        }).toList();
    }

    private GameDetailResponse toResponse(Game game) {
        final Map<Long, TeamResponse> teamsById = teamRepository
                .findByIds(List.of(game.getHomeTeamId(), game.getAwayTeamId())).stream()
                .collect(Collectors.toMap(Team::getId, TeamMapper::toResponse));
        return GameMapper.toResponse(game, teamsById);
    }
}

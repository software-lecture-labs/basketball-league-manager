package com.basketballleague.service;

import com.basketballleague.domain.SeasonNotFoundException;
import com.basketballleague.domain.TeamNotInSeasonException;
import com.basketballleague.domain.TeamNotFoundException;
import com.basketballleague.dto.SeasonResponse;
import com.basketballleague.dto.StandingResponse;
import com.basketballleague.dto.TeamRecentGameResponse;
import com.basketballleague.dto.TeamResponse;
import com.basketballleague.dto.TeamSeasonOverviewResponse;
import com.basketballleague.entity.Game;
import com.basketballleague.entity.Season;
import com.basketballleague.entity.Team;
import com.basketballleague.repository.GameRepository;
import com.basketballleague.repository.SeasonRepository;
import com.basketballleague.repository.TeamRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Read-only team views across seasons, including a season's standings row and recent confirmed games.
 */
@ApplicationScoped
public class TeamSeasonService {

    private static final int RECENT_GAME_LIMIT = 5;

    private final TeamRepository teamRepository;
    private final SeasonRepository seasonRepository;
    private final GameRepository gameRepository;
    private final StandingService standingService;

    /** Required by CDI to create the proxy; never used to serve calls. */
    protected TeamSeasonService() {
        this.teamRepository = null;
        this.seasonRepository = null;
        this.gameRepository = null;
        this.standingService = null;
    }

    @Inject
    public TeamSeasonService(TeamRepository teamRepository, SeasonRepository seasonRepository,
            GameRepository gameRepository, StandingService standingService) {
        this.teamRepository = Objects.requireNonNull(teamRepository, "teamRepository");
        this.seasonRepository = Objects.requireNonNull(seasonRepository, "seasonRepository");
        this.gameRepository = Objects.requireNonNull(gameRepository, "gameRepository");
        this.standingService = Objects.requireNonNull(standingService, "standingService");
    }

    public List<SeasonResponse> listSeasons(long teamId) {
        requireTeam(teamId);
        return seasonRepository.findByTeamId(teamId).stream()
                .map(SeasonMapper::toResponse)
                .toList();
    }

    public TeamSeasonOverviewResponse getOverview(long teamId, long seasonId) {
        requireTeam(teamId);
        final Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new SeasonNotFoundException(seasonId));
        if (!seasonRepository.findTeamIds(seasonId).contains(teamId)) {
            throw new TeamNotInSeasonException(teamId, seasonId);
        }

        final StandingResponse summary = standingService.calculateStandings(seasonId).stream()
                .filter(row -> row.teamId() == teamId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Team " + teamId + " is missing from season " + seasonId + " standings"));
        final List<Game> games = gameRepository.findRecentConfirmedBySeasonAndTeamId(
                seasonId, teamId, RECENT_GAME_LIMIT);
        final Set<Long> teamIds = games.stream()
                .flatMap(game -> Stream.of(game.getHomeTeamId(), game.getAwayTeamId()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        final Map<Long, TeamResponse> teamsById = teamRepository.findByIds(teamIds).stream()
                .map(TeamMapper::toResponse)
                .collect(Collectors.toMap(TeamResponse::id, Function.identity()));
        final List<TeamRecentGameResponse> recentGames = games.stream()
                .map(game -> new TeamRecentGameResponse(game.getId(), game.getRoundNumber(),
                        teamsById.get(game.getHomeTeamId()), teamsById.get(game.getAwayTeamId()),
                        Objects.requireNonNull(game.getHomeScore(), "Confirmed game must have a home score"),
                        Objects.requireNonNull(game.getAwayScore(), "Confirmed game must have an away score")))
                .toList();

        return new TeamSeasonOverviewResponse(SeasonMapper.toResponse(season), summary, recentGames);
    }

    private void requireTeam(long teamId) {
        if (teamRepository.findById(teamId).isEmpty()) {
            throw new TeamNotFoundException(teamId);
        }
    }
}

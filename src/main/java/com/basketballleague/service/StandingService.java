package com.basketballleague.service;

import com.basketballleague.domain.MatchResult;
import com.basketballleague.domain.PointsPolicy;
import com.basketballleague.domain.SeasonNotFoundException;
import com.basketballleague.domain.StandardPointsPolicy;
import com.basketballleague.domain.StandingsTable;
import com.basketballleague.domain.TeamStanding;
import com.basketballleague.dto.StandingResponse;
import com.basketballleague.entity.Game;
import com.basketballleague.repository.GameRepository;
import com.basketballleague.repository.SeasonRepository;
import com.basketballleague.repository.TeamRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The league table of a season, computed from its confirmed games on every read (ADR-0001). Ranking rules live in
 * {@link StandingsTable}; this class loads the data with a fixed number of queries, whatever the season's size.
 */
@ApplicationScoped
public class StandingService {

    private static final PointsPolicy POINTS_POLICY = StandardPointsPolicy.INSTANCE;

    private final SeasonRepository seasonRepository;
    private final GameRepository gameRepository;
    private final TeamRepository teamRepository;

    /** Required by CDI to create the proxy; never used to serve calls. */
    protected StandingService() {
        this.seasonRepository = null;
        this.gameRepository = null;
        this.teamRepository = null;
    }

    @Inject
    public StandingService(SeasonRepository seasonRepository, GameRepository gameRepository,
            TeamRepository teamRepository) {
        this.seasonRepository = Objects.requireNonNull(seasonRepository, "seasonRepository");
        this.gameRepository = Objects.requireNonNull(gameRepository, "gameRepository");
        this.teamRepository = Objects.requireNonNull(teamRepository, "teamRepository");
    }

    /**
     * Four queries: the season, its team ids, its teams and its confirmed games. Teams without a confirmed game are
     * listed with zeros.
     */
    public List<StandingResponse> calculateStandings(long seasonId) {
        seasonRepository.findById(seasonId).orElseThrow(() -> new SeasonNotFoundException(seasonId));
        final Map<Long, String> teamNames = new LinkedHashMap<>();
        teamRepository.findByIds(seasonRepository.findTeamIds(seasonId))
                .forEach(team -> teamNames.put(team.getId(), team.getName()));
        final List<MatchResult> results = gameRepository.findConfirmedBySeasonId(seasonId).stream()
                .map(StandingService::toResult)
                .toList();

        final List<TeamStanding> table = StandingsTable.rank(teamNames, results, POINTS_POLICY);
        final List<StandingResponse> response = new ArrayList<>(table.size());
        for (int index = 0; index < table.size(); index++) {
            response.add(toResponse(index + 1, table.get(index)));
        }
        return response;
    }

    /**
     * A confirmed game always has both scores: {@code Game.finish()} writes them and {@code Game.confirm()} checks
     * them (ADR-0004), so the stored values are read as they are.
     */
    private static MatchResult toResult(Game game) {
        return new MatchResult(game.getHomeTeamId(), game.getAwayTeamId(), game.getHomeScore(), game.getAwayScore());
    }

    private static StandingResponse toResponse(int rank, TeamStanding standing) {
        return new StandingResponse(rank, standing.teamId(), standing.teamName(), standing.played(), standing.won(),
                standing.lost(), standing.pointsScored(), standing.pointsConceded(), standing.pointDifference(),
                standing.points());
    }
}

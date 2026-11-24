package com.basketballleague.service;

import com.basketballleague.domain.MatchResult;
import com.basketballleague.domain.PointsPolicy;
import com.basketballleague.domain.SeasonNotFoundException;
import com.basketballleague.domain.StandardPointsPolicy;
import com.basketballleague.domain.StandingsComparator;
import com.basketballleague.domain.TeamStanding;
import com.basketballleague.dto.StandingResponse;
import com.basketballleague.entity.Game;
import com.basketballleague.entity.Team;
import com.basketballleague.repository.GameRepository;
import com.basketballleague.repository.SeasonRepository;
import com.basketballleague.repository.TeamRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Computes league standings dynamically from confirmed matches (ADR-0001).
 * Standings are not stored in a database table.
 */
@ApplicationScoped
public class StandingService {

    private final SeasonRepository seasonRepository;
    private final TeamRepository teamRepository;
    private final GameRepository gameRepository;
    private final PointsPolicy pointsPolicy;

    /** Required by CDI to create the proxy; never used to serve calls. */
    protected StandingService() {
        this.seasonRepository = null;
        this.teamRepository = null;
        this.gameRepository = null;
        this.pointsPolicy = null;
    }

    @Inject
    public StandingService(
            SeasonRepository seasonRepository,
            TeamRepository teamRepository,
            GameRepository gameRepository) {
        this(seasonRepository, teamRepository, gameRepository, StandardPointsPolicy.INSTANCE);
    }

    public StandingService(
            SeasonRepository seasonRepository,
            TeamRepository teamRepository,
            GameRepository gameRepository,
            PointsPolicy pointsPolicy) {
        this.seasonRepository = Objects.requireNonNull(seasonRepository, "seasonRepository");
        this.teamRepository = Objects.requireNonNull(teamRepository, "teamRepository");
        this.gameRepository = Objects.requireNonNull(gameRepository, "gameRepository");
        this.pointsPolicy = Objects.requireNonNull(pointsPolicy, "pointsPolicy");
    }

    public List<StandingResponse> calculateStandings(long seasonId) {
        // 1. Verify season existence (throws 404 SeasonNotFoundException if missing)
        if (seasonRepository.findById(seasonId).isEmpty()) {
            throw new SeasonNotFoundException(seasonId);
        }

        // 2. Load all teams (teams without matches must still appear with 0 points)
        final List<Team> teams = teamRepository.findAll();

        // 3. Load only CONFIRMED games for this season
        final List<Game> confirmedGames = gameRepository.findConfirmedGamesBySeason(seasonId);

        // 4. Extract domain MatchResult instances for head-to-head tie breaking
        final List<MatchResult> matchResults = confirmedGames.stream()
                .map(g -> new MatchResult(
                        g.getHomeTeam().getId(),
                        g.getAwayTeam().getId(),
                        g.getHomeScore(),
                        g.getAwayScore()))
                .toList();

        // 5. Accumulate match statistics per team
        final Map<Long, TeamStatsAccumulator> statsMap = new HashMap<>();
        for (Team team : teams) {
            statsMap.put(team.getId(), new TeamStatsAccumulator(team.getId(), team.getName()));
        }

        for (Game game : confirmedGames) {
            final Long homeId = game.getHomeTeam().getId();
            final Long awayId = game.getAwayTeam().getId();
            final int homeScore = game.getHomeScore();
            final int awayScore = game.getAwayScore();

            final TeamStatsAccumulator homeStats = statsMap.get(homeId);
            final TeamStatsAccumulator awayStats = statsMap.get(awayId);

            if (homeStats != null && awayStats != null) {
                homeStats.addMatch(homeScore, awayScore);
                awayStats.addMatch(awayScore, homeScore);
            }
        }

        // 6. Map to domain TeamStanding instances
        final List<TeamStanding> standingsList = new ArrayList<>();
        for (TeamStatsAccumulator stats : statsMap.values()) {
            standingsList.add(TeamStanding.of(
                    stats.teamId,
                    stats.teamName,
                    stats.wins,
                    stats.losses,
                    stats.pointsScored,
                    stats.pointsConceded,
                    pointsPolicy
            ));
        }

        // 7. Sort using TieBreaker chain: points -> H2H -> point difference -> points scored -> team name
        final StandingsComparator comparator = StandingsComparator.ofStandard(matchResults, pointsPolicy);
        standingsList.sort(comparator);

        // 8. Assign ranks (1..N) and map to response DTOs
        final List<StandingResponse> responseList = new ArrayList<>();
        int currentRank = 1;
        for (TeamStanding ts : standingsList) {
            responseList.add(new StandingResponse(
                    currentRank++,
                    ts.teamId(),
                    ts.teamName(),
                    ts.played(),
                    ts.won(),
                    ts.lost(),
                    ts.pointsScored(),
                    ts.pointsConceded(),
                    ts.pointDifference(),
                    ts.points()
            ));
        }

        return responseList;
    }

    private static final class TeamStatsAccumulator {
        private final Long teamId;
        private final String teamName;
        private int wins = 0;
        private int losses = 0;
        private int pointsScored = 0;
        private int pointsConceded = 0;

        private TeamStatsAccumulator(Long teamId, String teamName) {
            this.teamId = teamId;
            this.teamName = teamName;
        }

        private void addMatch(int scored, int conceded) {
            pointsScored += scored;
            pointsConceded += conceded;
            if (scored > conceded) {
                wins++;
            } else {
                losses++;
            }
        }
    }
}

package com.basketballleague.service;

import com.basketballleague.domain.MatchResult;
import com.basketballleague.domain.PointsPolicy;
import com.basketballleague.domain.SeasonNotFoundException;
import com.basketballleague.domain.StandardPointsPolicy;
import com.basketballleague.domain.StandingsComparator;
import com.basketballleague.domain.TeamStanding;
import com.basketballleague.dto.StandingResponse;
import com.basketballleague.entity.Game;
import com.basketballleague.entity.Season;
import com.basketballleague.entity.Team;
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
 * Computes the league standings for a season from its CONFIRMED games.
 *
 * <h2>Contract with Osman's 3.2 (score entry)</h2>
 * <p>Score entry will add {@code homeScore} / {@code awayScore} to the {@link Game} entity
 * and transition status to {@code CONFIRMED}.
 * This service reads confirmed games via {@link GameRepository#findConfirmedBySeasonId(long)};
 * once 3.2 is merged standings update automatically — no changes needed here except
 * the three {@code TODO(3.2)} stub methods below.
 *
 * <h2>Score fields</h2>
 * <p>Until 3.2 lands, {@link #hasScores(Game)} returns {@code false} so the aggregate
 * loop is skipped. All teams appear in the table with zero stats.
 */
@ApplicationScoped
public class StandingService {

    private static final PointsPolicy POINTS_POLICY = StandardPointsPolicy.INSTANCE;

    private final SeasonRepository seasonRepository;
    private final GameRepository gameRepository;
    private final TeamRepository teamRepository;

    /** Required by CDI proxy. */
    protected StandingService() {
        this.seasonRepository = null;
        this.gameRepository = null;
        this.teamRepository = null;
    }

    @Inject
    public StandingService(SeasonRepository seasonRepository,
                           GameRepository gameRepository,
                           TeamRepository teamRepository) {
        this.seasonRepository = Objects.requireNonNull(seasonRepository, "seasonRepository");
        this.gameRepository = Objects.requireNonNull(gameRepository, "gameRepository");
        this.teamRepository = Objects.requireNonNull(teamRepository, "teamRepository");
    }

    /**
     * Computes the standings for the given season.
     *
     * <ol>
     *   <li>Verifies the season exists.</li>
     *   <li>Loads the team roster (IDs → names) in one query.</li>
     *   <li>Loads all CONFIRMED games in one query.</li>
     *   <li>Accumulates per-team stats.</li>
     *   <li>Sorts with {@link StandingsComparator} (H2H → point diff → points scored → name).</li>
     *   <li>Returns one {@link StandingResponse} per team, ranked 1-based.</li>
     * </ol>
     *
     * @throws SeasonNotFoundException if {@code seasonId} does not exist.
     */
    public List<StandingResponse> calculateStandings(long seasonId) {
        // 1. Season must exist
        final Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new SeasonNotFoundException(seasonId));

        // 2. Load team names in a single query (teamId → name)
        final List<Long> teamIds = new ArrayList<>(season.getTeamIds());
        final Map<Long, String> teamNames = new LinkedHashMap<>();
        if (!teamIds.isEmpty()) {
            teamRepository.findByIds(teamIds)
                    .forEach(t -> teamNames.put(t.getId(), t.getName()));
        }

        // 3. Fetch confirmed games (single query)
        final List<Game> confirmedGames = gameRepository.findConfirmedBySeasonId(seasonId);

        // 4. Build match results for the tie-breaker chain (empty until 3.2)
        final List<MatchResult> matchResults = new ArrayList<>();
        for (Game g : confirmedGames) {
            final MatchResult mr = buildMatchResult(g);
            if (mr != null) {
                matchResults.add(mr);
            }
        }

        // 5. Build aggregate standings — every team starts at zero
        final Map<Long, TeamStanding> aggregates = new LinkedHashMap<>();
        for (Long tid : teamIds) {
            final String name = teamNames.getOrDefault(tid, "Team " + tid);
            aggregates.put(tid, TeamStanding.zero(tid, name));
        }

        for (Game g : confirmedGames) {
            if (!hasScores(g)) {
                continue; // skip until 3.2 lands
            }
            final long homeId = g.getHomeTeamId();
            final long awayId = g.getAwayTeamId();
            final int homeScore = getHomeScore(g);
            final int awayScore = getAwayScore(g);

            aggregates.computeIfPresent(homeId,
                    (id, prev) -> addGameResult(prev, homeScore, awayScore, POINTS_POLICY));
            aggregates.computeIfPresent(awayId,
                    (id, prev) -> addGameResult(prev, awayScore, homeScore, POINTS_POLICY));
        }

        // 6. Sort
        final StandingsComparator comparator = StandingsComparator.ofStandard(matchResults, POINTS_POLICY);
        final List<TeamStanding> sorted = new ArrayList<>(aggregates.values());
        sorted.sort(comparator);

        // 7. Map to response (rank = index + 1)
        final List<StandingResponse> result = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            result.add(toResponse(i + 1, sorted.get(i)));
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Integration point for Osman's 3.2: replace these three stubs when Game
    // exposes homeScore / awayScore fields.
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} when score fields are present on the entity.
     * <strong>TODO(3.2):</strong> {@code return game.getHomeScore() != null && game.getAwayScore() != null;}
     */
    private boolean hasScores(Game game) {
        return false;
    }

    /**
     * Reads home score from the game entity.
     * <strong>TODO(3.2):</strong> {@code return game.getHomeScore();}
     */
    private int getHomeScore(Game game) {
        throw new UnsupportedOperationException("Score fields not available until 3.2 is merged");
    }

    /**
     * Reads away score from the game entity.
     * <strong>TODO(3.2):</strong> {@code return game.getAwayScore();}
     */
    private int getAwayScore(Game game) {
        throw new UnsupportedOperationException("Score fields not available until 3.2 is merged");
    }

    private MatchResult buildMatchResult(Game game) {
        if (!hasScores(game)) {
            return null;
        }
        return new MatchResult(game.getHomeTeamId(), game.getAwayTeamId(),
                getHomeScore(game), getAwayScore(game));
    }

    // -------------------------------------------------------------------------
    // Pure helpers
    // -------------------------------------------------------------------------

    private static TeamStanding addGameResult(TeamStanding prev,
                                               int pointsFor, int pointsAgainst,
                                               PointsPolicy policy) {
        final boolean won = pointsFor > pointsAgainst;
        final int newWon = prev.won() + (won ? 1 : 0);
        final int newLost = prev.lost() + (won ? 0 : 1);
        final int newPf = prev.pointsScored() + pointsFor;
        final int newPa = prev.pointsConceded() + pointsAgainst;
        return TeamStanding.of(prev.teamId(), prev.teamName(), newWon, newLost, newPf, newPa, policy);
    }

    private static StandingResponse toResponse(int rank, TeamStanding s) {
        return new StandingResponse(
                rank,
                s.teamId(),
                s.teamName(),
                s.played(),
                s.won(),
                s.lost(),
                s.pointsScored(),
                s.pointsConceded(),
                s.pointDifference(),
                s.points()
        );
    }
}

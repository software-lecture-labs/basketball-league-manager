package com.basketballleague.service;

import com.basketballleague.dto.GameDetailResponse;
import com.basketballleague.dto.PlayerPointsResponse;
import com.basketballleague.dto.QuarterScoreRequest;
import com.basketballleague.dto.QuarterScoreResponse;
import com.basketballleague.dto.TeamResponse;
import com.basketballleague.entity.Game;
import com.basketballleague.entity.QuarterScore;
import java.util.List;
import java.util.Map;

/**
 * Converts between the game aggregate and its REST records. The two teams are loaded beforehand in one query.
 */
final class GameMapper {

    private GameMapper() {
    }

    static GameDetailResponse toResponse(Game game, Map<Long, TeamResponse> teamsById) {
        return new GameDetailResponse(game.getId(), game.getSeasonId(), game.getRoundNumber(),
                teamsById.get(game.getHomeTeamId()), teamsById.get(game.getAwayTeamId()), game.getStatus().name(),
                game.getHomeScore(), game.getAwayScore(),
                game.getQuarterScores().stream()
                        .map(q -> new QuarterScoreResponse(q.getQuarterNo(), q.getHomePoints(), q.getAwayPoints()))
                        .toList(),
                game.getPlayerPoints().stream()
                        .map(p -> new PlayerPointsResponse(p.getPlayerId(), p.getTeamId(), p.getPoints()))
                        .toList(),
                game.getVersion());
    }

    static List<QuarterScore> toQuarterScores(List<QuarterScoreRequest> quarters) {
        return quarters.stream()
                .map(q -> new QuarterScore(q.quarterNo(), q.homePoints(), q.awayPoints()))
                .toList();
    }
}

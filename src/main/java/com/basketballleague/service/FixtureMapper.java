package com.basketballleague.service;

import com.basketballleague.dto.FixtureResponse;
import com.basketballleague.dto.GameResponse;
import com.basketballleague.dto.RoundResponse;
import com.basketballleague.dto.TeamResponse;
import com.basketballleague.entity.Game;
import com.basketballleague.entity.Team;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds the fixture response from games and the season's teams, which are loaded once beforehand. The bye
 * team is not stored; it is the one season team that plays no game in the round.
 */
final class FixtureMapper {

    private FixtureMapper() {
    }

    /**
     * {@code games} must be ordered by round, as {@code GameRepository.findBySeasonId} returns them.
     */
    static FixtureResponse toResponse(long seasonId, List<Game> games, Collection<Team> seasonTeams) {
        final Map<Long, TeamResponse> teamsById = seasonTeams.stream()
                .collect(Collectors.toMap(Team::getId, TeamMapper::toResponse));
        final Map<Integer, List<Game>> gamesByRound = games.stream()
                .collect(Collectors.groupingBy(Game::getRoundNumber, LinkedHashMap::new, Collectors.toList()));

        final List<RoundResponse> rounds = new ArrayList<>();
        gamesByRound.forEach((number, roundGames) -> rounds.add(new RoundResponse(number,
                roundGames.stream().map(game -> toResponse(game, teamsById)).toList(),
                byeTeam(roundGames, teamsById))));
        return new FixtureResponse(seasonId, rounds);
    }

    private static GameResponse toResponse(Game game, Map<Long, TeamResponse> teamsById) {
        return new GameResponse(game.getId(), teamsById.get(game.getHomeTeamId()),
                teamsById.get(game.getAwayTeamId()), game.getStatus().name());
    }

    private static TeamResponse byeTeam(List<Game> roundGames, Map<Long, TeamResponse> teamsById) {
        final Set<Long> resting = new HashSet<>(teamsById.keySet());
        for (Game game : roundGames) {
            resting.remove(game.getHomeTeamId());
            resting.remove(game.getAwayTeamId());
        }
        return resting.size() == 1 ? teamsById.get(resting.iterator().next()) : null;
    }
}

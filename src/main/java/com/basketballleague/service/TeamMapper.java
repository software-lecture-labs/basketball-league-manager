package com.basketballleague.service;

import com.basketballleague.dto.PlayerResponse;
import com.basketballleague.dto.TeamResponse;
import com.basketballleague.entity.Player;
import com.basketballleague.entity.Team;

/**
 * Converts team and player entities to REST responses, so entities never leave the service layer (ADR-0001).
 */
final class TeamMapper {

    private TeamMapper() {
    }

    static TeamResponse toResponse(Team team) {
        return new TeamResponse(team.getId(), team.getName(), team.getCity());
    }

    static PlayerResponse toResponse(Player player) {
        return new PlayerResponse(player.getId(), player.getFirstName(), player.getLastName(),
                player.getJerseyNumber(), player.getPosition().name());
    }
}

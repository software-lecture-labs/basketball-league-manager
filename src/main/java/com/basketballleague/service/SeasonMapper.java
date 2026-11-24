package com.basketballleague.service;

import com.basketballleague.dto.SeasonResponse;
import com.basketballleague.entity.Season;

/**
 * Converts season entities to REST responses, so entities never leave the service layer (ADR-0001).
 */
final class SeasonMapper {

    private SeasonMapper() {
    }

    static SeasonResponse toResponse(Season season) {
        return new SeasonResponse(season.getId(), season.getName(), season.getStartDate(),
                season.getStatus().name());
    }
}

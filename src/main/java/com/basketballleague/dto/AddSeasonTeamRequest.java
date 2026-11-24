package com.basketballleague.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Body of {@code POST /seasons/{id}/teams}: the existing team that joins the season.
 */
public record AddSeasonTeamRequest(@NotNull @Positive Long teamId) {
}

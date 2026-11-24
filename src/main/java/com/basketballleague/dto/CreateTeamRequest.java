package com.basketballleague.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /teams}. The limits match the {@code teams} table columns.
 */
public record CreateTeamRequest(
        @NotBlank @Size(max = TextLimits.NAME_MAX_LENGTH) String name,
        @NotBlank @Size(max = TextLimits.NAME_MAX_LENGTH) String city) {
}

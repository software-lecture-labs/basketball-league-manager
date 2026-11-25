package com.basketballleague.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /teams/{id}/players}. The limits match the {@code players} table columns and checks.
 */
public record CreatePlayerRequest(
        @NotBlank @Size(max = TextLimits.NAME_MAX_LENGTH) String firstName,
        @NotBlank @Size(max = TextLimits.NAME_MAX_LENGTH) String lastName,
        @NotNull @Min(MIN_JERSEY_NUMBER) @Max(MAX_JERSEY_NUMBER) Integer jerseyNumber,
        @NotNull @Pattern(regexp = POSITIONS, message = "PG, SG, SF, PF veya C olmalıdır") String position) {

    static final int MIN_JERSEY_NUMBER = 0;
    static final int MAX_JERSEY_NUMBER = 99;
    static final String POSITIONS = "PG|SG|SF|PF|C";
}

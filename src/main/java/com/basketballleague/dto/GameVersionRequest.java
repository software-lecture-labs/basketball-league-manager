package com.basketballleague.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Body of {@code POST /games/{id}/start|finish|reopen|confirm}: the game version the client last read (ADR-0004).
 */
public record GameVersionRequest(@NotNull @PositiveOrZero Long version) {
}

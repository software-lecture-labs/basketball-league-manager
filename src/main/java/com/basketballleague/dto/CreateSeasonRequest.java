package com.basketballleague.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Body of {@code POST /seasons}. {@code startDate} is an ISO-8601 date such as {@code 2026-10-01}.
 */
public record CreateSeasonRequest(
        @NotBlank @Size(max = TextLimits.NAME_MAX_LENGTH) String name,
        @NotNull LocalDate startDate) {
}

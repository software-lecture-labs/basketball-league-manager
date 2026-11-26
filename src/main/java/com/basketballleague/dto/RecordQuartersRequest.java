package com.basketballleague.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Body of {@code PUT /games/{id}/quarters}: the whole quarter list replaces the stored one. At most four quarters
 * and six overtime periods.
 */
public record RecordQuartersRequest(
        @NotNull @PositiveOrZero Long version,
        @NotNull @Size(max = MAX_QUARTERS) List<@NotNull @Valid QuarterScoreRequest> quarters) {

    static final int MAX_QUARTERS = 10;
}

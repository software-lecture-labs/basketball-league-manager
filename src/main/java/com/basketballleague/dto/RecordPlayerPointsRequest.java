package com.basketballleague.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Body of {@code PUT /games/{id}/stats}: the whole list replaces the stored player points. Room for two full
 * rosters.
 */
public record RecordPlayerPointsRequest(
        @NotNull @PositiveOrZero Long version,
        @NotNull @Size(max = MAX_PLAYERS) List<@NotNull @Valid PlayerPointsRequest> players) {

    static final int MAX_PLAYERS = 40;

    /**
     * A player listed twice is a client error (400), as docs/api.md says. Missing ids are reported by their own
     * field rules, so they are skipped here.
     */
    @AssertTrue(message = "her oyuncu listede bir kez yer almalıdır")
    public boolean isEachPlayerListedOnce() {
        if (players == null) {
            return true;
        }
        final Set<Long> seen = new HashSet<>();
        return players.stream()
                .filter(Objects::nonNull)
                .map(PlayerPointsRequest::playerId)
                .filter(Objects::nonNull)
                .allMatch(seen::add);
    }
}

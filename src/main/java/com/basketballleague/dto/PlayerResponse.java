package com.basketballleague.dto;

import jakarta.json.bind.annotation.JsonbPropertyOrder;

/**
 * A player as returned by the REST API. {@code position} is one of PG, SG, SF, PF, C.
 */
@JsonbPropertyOrder({"id", "firstName", "lastName", "jerseyNumber", "position"})
public record PlayerResponse(long id, String firstName, String lastName, int jerseyNumber, String position) {
}

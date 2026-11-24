package com.basketballleague.dto;

import jakarta.json.bind.annotation.JsonbPropertyOrder;

/**
 * A team as returned by the REST API.
 */
@JsonbPropertyOrder({"id", "name", "city"})
public record TeamResponse(long id, String name, String city) {
}

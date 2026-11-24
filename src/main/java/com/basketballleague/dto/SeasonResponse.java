package com.basketballleague.dto;

import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.time.LocalDate;

/**
 * A season as returned by the REST API. {@code status} is one of DRAFT, ACTIVE, FINISHED.
 */
@JsonbPropertyOrder({"id", "name", "startDate", "status"})
public record SeasonResponse(long id, String name, LocalDate startDate, String status) {
}

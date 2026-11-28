package com.basketballleague.dto;

import jakarta.json.bind.annotation.JsonbPropertyOrder;

/**
 * Points of both teams in one quarter; quarter 5 and later are overtime periods.
 */
@JsonbPropertyOrder({"quarterNo", "homePoints", "awayPoints"})
public record QuarterScoreResponse(int quarterNo, int homePoints, int awayPoints) {
}

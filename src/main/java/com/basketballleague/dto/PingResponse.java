package com.basketballleague.dto;

import java.time.Instant;

/**
 * Body of {@code GET /api/ping}; the timestamp is serialized as ISO-8601 UTC.
 */
public record PingResponse(String status, Instant timestamp) {
}

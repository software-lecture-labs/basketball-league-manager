package com.basketballleague.domain;

/**
 * Stable identifiers of business rule violations. The REST layer sends the name as the {@code code} member
 * of a problem details response, so a value must not be renamed once clients use it.
 */
public enum ErrorCode {
    INVALID_FIXTURE_INPUT,
    INVALID_TEAM,
    INVALID_PLAYER,
    TEAM_NOT_FOUND,
    TEAM_NAME_TAKEN,
    JERSEY_NUMBER_TAKEN,
    INVALID_GAME_STATUS_TRANSITION
}

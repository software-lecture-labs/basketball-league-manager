package com.basketballleague.dto;

/**
 * Text column lengths shared by request records; they mirror {@code varchar(100)} in the V1 migration.
 */
final class TextLimits {

    static final int NAME_MAX_LENGTH = 100;

    private TextLimits() {
    }
}

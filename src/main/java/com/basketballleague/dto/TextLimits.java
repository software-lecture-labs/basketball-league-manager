package com.basketballleague.dto;

/**
 * Text column lengths shared by request records; they mirror the {@code varchar(100)} name columns of the
 * migrations.
 */
final class TextLimits {

    static final int NAME_MAX_LENGTH = 100;

    private TextLimits() {
    }
}

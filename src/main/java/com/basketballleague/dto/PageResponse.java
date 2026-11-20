package com.basketballleague.dto;

import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.util.List;

/**
 * One page of a list endpoint. {@code page} starts at 0; {@code totalItems} counts all records, not only this page.
 */
@JsonbPropertyOrder({"items", "page", "size", "totalItems"})
public record PageResponse<T>(List<T> items, int page, int size, long totalItems) {

    public PageResponse {
        items = List.copyOf(items);
    }
}

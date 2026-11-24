package com.basketballleague.resource;

/**
 * Paging limits shared by the list endpoints (docs/api.md, "Ortak kurallar").
 */
final class PageLimits {

    static final String DEFAULT_PAGE_SIZE = "20";
    static final int MAX_PAGE_SIZE = 100;
    /** Keeps {@code page * size} inside an int, the type JPA uses for the first result. */
    static final int MAX_PAGE = Integer.MAX_VALUE / MAX_PAGE_SIZE;

    private PageLimits() {
    }
}

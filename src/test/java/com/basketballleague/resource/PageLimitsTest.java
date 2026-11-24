package com.basketballleague.resource;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PageLimitsTest {

    @Test
    void largestAllowedPageStillFitsInAnInt() {
        assertThat((long) PageLimits.MAX_PAGE * PageLimits.MAX_PAGE_SIZE).isLessThanOrEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void defaultPageSizeIsWithinTheLimit() {
        assertThat(Integer.parseInt(PageLimits.DEFAULT_PAGE_SIZE)).isBetween(1, PageLimits.MAX_PAGE_SIZE);
    }
}

package com.basketballleague.domain;

import java.text.Collator;
import java.util.Locale;
import java.util.Objects;

/**
 * Final tie-breaker rule based on team name in alphabetical order (A -> Z).
 * Uses a Turkish locale-aware {@link Collator} so that names starting with
 * "İ", "Ö", "Ş", "Ü", etc. sort correctly (e.g. "İstanbul" before "J…").
 */
public final class TeamNameTieBreaker implements TieBreaker {

    private static final Collator TR_COLLATOR;

    static {
        TR_COLLATOR = Collator.getInstance(new Locale("tr", "TR"));
        TR_COLLATOR.setStrength(Collator.PRIMARY);
    }

    @Override
    public int breakTie(TeamStanding a, TeamStanding b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        return TR_COLLATOR.compare(a.teamName(), b.teamName());
    }
}

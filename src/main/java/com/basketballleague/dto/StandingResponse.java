package com.basketballleague.dto;

/**
 * One row in the standings table returned by {@code GET /api/seasons/{id}/standings}.
 *
 * <p>All fields are computed from CONFIRMED games; no data is persisted separately (ADR-0003).
 *
 * @param rank           Position in the league table (1-based).
 * @param teamId         Persistent team identifier.
 * @param teamName       Display name of the team.
 * @param played         Number of games played (won + lost; no draws in basketball).
 * @param won            Number of wins.
 * @param lost           Number of losses.
 * @param pointsFor      Total points scored by this team.
 * @param pointsAgainst  Total points conceded by this team.
 * @param pointDiff      {@code pointsFor - pointsAgainst}.
 * @param leaguePoints   League points: win = 2, loss = 1 (see {@link com.basketballleague.domain.StandardPointsPolicy}).
 */
public record StandingResponse(
        int rank,
        long teamId,
        String teamName,
        int played,
        int won,
        int lost,
        int pointsFor,
        int pointsAgainst,
        int pointDiff,
        int leaguePoints
) {
}

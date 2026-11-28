package com.basketballleague.service;

import static com.basketballleague.support.EntityFields.withField;
import static com.basketballleague.support.EntityIds.withId;
import static org.assertj.core.api.Assertions.assertThat;

import com.basketballleague.dto.GameDetailResponse;
import com.basketballleague.dto.PlayerPointsResponse;
import com.basketballleague.dto.QuarterScoreRequest;
import com.basketballleague.dto.QuarterScoreResponse;
import com.basketballleague.dto.TeamResponse;
import com.basketballleague.entity.Game;
import com.basketballleague.entity.PlayerPoints;
import com.basketballleague.entity.QuarterScore;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GameMapperTest {

    private static final long HOME = 1L;
    private static final long AWAY = 9L;
    private static final Map<Long, TeamResponse> TEAMS = Map.of(
            HOME, new TeamResponse(HOME, "Anadolu Efes", "İstanbul"),
            AWAY, new TeamResponse(AWAY, "Fenerbahçe Beko", "İstanbul"));

    @Test
    void mapsAScheduledGameWithoutScoresAndWithEmptyLists() {
        final Game game = withId(new Game(3L, 2, HOME, AWAY), 10L);

        final GameDetailResponse response = GameMapper.toResponse(game, TEAMS);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.seasonId()).isEqualTo(3L);
        assertThat(response.roundNumber()).isEqualTo(2);
        assertThat(response.homeTeam()).isEqualTo(TEAMS.get(HOME));
        assertThat(response.awayTeam()).isEqualTo(TEAMS.get(AWAY));
        assertThat(response.status()).isEqualTo("SCHEDULED");
        assertThat(response.homeScore()).isNull();
        assertThat(response.awayScore()).isNull();
        assertThat(response.quarters()).isEmpty();
        assertThat(response.playerPoints()).isEmpty();
    }

    @Test
    void mapsAFinishedGamesQuartersPlayerPointsAndVersion() {
        final Game game = withId(new Game(3L, 1, HOME, AWAY), 10L);
        game.start();
        game.recordQuarters(List.of(new QuarterScore(1, 22, 18), new QuarterScore(2, 20, 21),
                new QuarterScore(3, 19, 20), new QuarterScore(4, 23, 21)));
        game.finish();
        game.recordPlayerPoints(List.of(new PlayerPoints(7L, HOME, 84), new PlayerPoints(40L, AWAY, 80)));
        withField(game, "version", 6L);

        final GameDetailResponse response = GameMapper.toResponse(game, TEAMS);

        assertThat(response.status()).isEqualTo("FINISHED");
        assertThat(response.homeScore()).isEqualTo(84);
        assertThat(response.awayScore()).isEqualTo(80);
        assertThat(response.quarters()).first().isEqualTo(new QuarterScoreResponse(1, 22, 18));
        assertThat(response.quarters()).hasSize(4);
        assertThat(response.playerPoints()).containsExactly(
                new PlayerPointsResponse(7L, HOME, 84), new PlayerPointsResponse(40L, AWAY, 80));
        assertThat(response.version()).isEqualTo(6L);
    }

    @Test
    void turnsQuarterRequestsIntoQuarterScores() {
        assertThat(GameMapper.toQuarterScores(List.of(new QuarterScoreRequest(1, 22, 18))))
                .containsExactly(new QuarterScore(1, 22, 18));
    }
}

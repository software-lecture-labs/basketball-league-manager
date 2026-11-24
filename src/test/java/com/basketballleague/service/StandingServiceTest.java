package com.basketballleague.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.basketballleague.domain.SeasonNotFoundException;
import com.basketballleague.domain.StandardPointsPolicy;
import com.basketballleague.dto.StandingResponse;
import com.basketballleague.entity.Game;
import com.basketballleague.entity.Season;
import com.basketballleague.entity.Team;
import com.basketballleague.repository.GameRepository;
import com.basketballleague.repository.SeasonRepository;
import com.basketballleague.repository.TeamRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StandingServiceTest {

    private final SeasonRepository seasonRepository = mock(SeasonRepository.class);
    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final GameRepository gameRepository = mock(GameRepository.class);
    private final StandingService standingService = new StandingService(
            seasonRepository, teamRepository, gameRepository, StandardPointsPolicy.INSTANCE);

    @Test
    @DisplayName("Var olmayan sezon sorgulandığında 404 SeasonNotFoundException fırlatılır")
    void throwsSeasonNotFoundExceptionWhenSeasonDoesNotExist() {
        when(seasonRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> standingService.calculateStandings(999L))
                .isInstanceOf(SeasonNotFoundException.class);
    }

    @Test
    @DisplayName("Onaylı maçlardan puan durumu doğru hesaplanır ve henüz maçı olmayan takımlar 0 puanla listelenir")
    void calculatesStandingsWithZeroStatsForUnplayedTeams() throws Exception {
        final long seasonId = 1L;
        final Season season = new Season("2025-26");
        setId(season, seasonId);

        final Team efes = new Team("Anadolu Efes", "İstanbul");
        setId(efes, 1L);
        final Team real = new Team("Real Madrid", "Madrid");
        setId(real, 2L);
        final Team milano = new Team("Olimpia Milano", "Milano");
        setId(milano, 3L);

        when(seasonRepository.findById(seasonId)).thenReturn(Optional.of(season));
        when(teamRepository.findAll()).thenReturn(List.of(efes, real, milano));

        // Real beat Efes 85 - 80
        final Game game = new Game(season, real, efes, 1);
        setId(game, 100L);
        game.recordScores(85, 80);
        game.confirm();

        when(gameRepository.findConfirmedGamesBySeason(seasonId)).thenReturn(List.of(game));

        final List<StandingResponse> standings = standingService.calculateStandings(seasonId);

        assertThat(standings).hasSize(3);

        // 1st: Real Madrid (1 played, 1 won, 2 pts, +5 diff)
        final StandingResponse rank1 = standings.get(0);
        assertThat(rank1.rank()).isEqualTo(1);
        assertThat(rank1.teamName()).isEqualTo("Real Madrid");
        assertThat(rank1.played()).isEqualTo(1);
        assertThat(rank1.won()).isEqualTo(1);
        assertThat(rank1.lost()).isEqualTo(0);
        assertThat(rank1.pointsScored()).isEqualTo(85);
        assertThat(rank1.pointsConceded()).isEqualTo(80);
        assertThat(rank1.pointDifference()).isEqualTo(5);
        assertThat(rank1.points()).isEqualTo(2);

        // 2nd: Anadolu Efes (1 played, 0 won, 1 lost, 1 pt, -5 diff)
        final StandingResponse rank2 = standings.get(1);
        assertThat(rank2.rank()).isEqualTo(2);
        assertThat(rank2.teamName()).isEqualTo("Anadolu Efes");
        assertThat(rank2.played()).isEqualTo(1);
        assertThat(rank2.won()).isEqualTo(0);
        assertThat(rank2.lost()).isEqualTo(1);
        assertThat(rank2.pointsScored()).isEqualTo(80);
        assertThat(rank2.pointsConceded()).isEqualTo(85);
        assertThat(rank2.pointDifference()).isEqualTo(-5);
        assertThat(rank2.points()).isEqualTo(1);

        // 3rd: Olimpia Milano (0 played, 0 won, 0 lost, 0 pts, 0 diff)
        final StandingResponse rank3 = standings.get(2);
        assertThat(rank3.rank()).isEqualTo(3);
        assertThat(rank3.teamName()).isEqualTo("Olimpia Milano");
        assertThat(rank3.played()).isEqualTo(0);
        assertThat(rank3.won()).isEqualTo(0);
        assertThat(rank3.lost()).isEqualTo(0);
        assertThat(rank3.points()).isEqualTo(0);
    }

    private static void setId(Object entity, Long id) throws Exception {
        final Field idField = entity.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }
}

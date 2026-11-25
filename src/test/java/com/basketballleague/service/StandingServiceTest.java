package com.basketballleague.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.basketballleague.domain.SeasonNotFoundException;
import com.basketballleague.dto.StandingResponse;
import com.basketballleague.entity.Season;
import com.basketballleague.entity.Team;
import com.basketballleague.repository.GameRepository;
import com.basketballleague.repository.SeasonRepository;
import com.basketballleague.repository.TeamRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link StandingService}.
 *
 * <h2>Pre-3.2 scope</h2>
 * Tests verify that:
 * <ul>
 *   <li>All season teams appear in the standings table with zero stats when no confirmed games exist.</li>
 *   <li>Teams are sorted alphabetically (Türkçe Collator via {@link StandingsComparator}) when all stats are equal.</li>
 *   <li>{@link SeasonNotFoundException} is thrown for an unknown season.</li>
 * </ul>
 *
 * <p>Score-dependent tests (non-zero wins, points, tie-breaking on point diff) will be added
 * when Osman's 3.2 (score entry) is merged and {@code hasScores()} returns {@code true}.
 */
class StandingServiceTest {

    private SeasonRepository seasonRepository;
    private GameRepository gameRepository;
    private TeamRepository teamRepository;
    private StandingService standingService;

    @BeforeEach
    void setUp() {
        seasonRepository = mock(SeasonRepository.class);
        gameRepository = mock(GameRepository.class);
        teamRepository = mock(TeamRepository.class);
        standingService = new StandingService(seasonRepository, gameRepository, teamRepository);
    }

    @Test
    @DisplayName("Bilinmeyen sezon için SeasonNotFoundException fırlatır")
    void throwsSeasonNotFoundForUnknownSeason() {
        when(seasonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> standingService.calculateStandings(99L))
                .isInstanceOf(SeasonNotFoundException.class);
    }

    @Test
    @DisplayName("Onaylı maç yoksa tüm takımlar sıfır istatistikle görünür")
    void allTeamsAppearsWithZeroStatsWhenNoConfirmedGames() {
        final Season season = new Season("EuroLeague 2025-26", LocalDate.of(2025, 10, 1));
        season.addTeam(1L);
        season.addTeam(2L);
        season.addTeam(3L);

        when(seasonRepository.findById(1L)).thenReturn(Optional.of(season));
        when(gameRepository.findConfirmedBySeasonId(1L)).thenReturn(List.of());
        when(teamRepository.findByIds(List.of(1L, 2L, 3L))).thenReturn(List.of(
                makeTeam(1L, "Anadolu Efes"),
                makeTeam(2L, "Fenerbahçe Beko"),
                makeTeam(3L, "Real Madrid")
        ));

        final List<StandingResponse> standings = standingService.calculateStandings(1L);

        assertThat(standings).hasSize(3);
        standings.forEach(s -> {
            assertThat(s.played()).isZero();
            assertThat(s.won()).isZero();
            assertThat(s.lost()).isZero();
            assertThat(s.pointsFor()).isZero();
            assertThat(s.pointsAgainst()).isZero();
            assertThat(s.leaguePoints()).isZero();
        });
    }

    @Test
    @DisplayName("Onaylı maç yokken takımlar alfabetik sırayla listelenir")
    void teamsAreRankedAlphabeticallyWhenNoGamesPlayed() {
        final Season season = new Season("Test", LocalDate.of(2025, 10, 1));
        season.addTeam(1L);
        season.addTeam(2L);
        season.addTeam(3L);

        when(seasonRepository.findById(1L)).thenReturn(Optional.of(season));
        when(gameRepository.findConfirmedBySeasonId(1L)).thenReturn(List.of());
        // Provide in reverse order to ensure sorting is by service, not by input order
        when(teamRepository.findByIds(List.of(1L, 2L, 3L))).thenReturn(List.of(
                makeTeam(3L, "Real Madrid"),
                makeTeam(2L, "Panathinaikos"),
                makeTeam(1L, "Anadolu Efes")
        ));

        final List<StandingResponse> standings = standingService.calculateStandings(1L);

        assertThat(standings).extracting(StandingResponse::teamName)
                .containsExactly("Anadolu Efes", "Panathinaikos", "Real Madrid");
    }

    @Test
    @DisplayName("rank alanı 1'den başlar ve ardışık olur")
    void rankIsOneBasedAndConsecutive() {
        final Season season = new Season("Test", LocalDate.of(2025, 10, 1));
        season.addTeam(1L);
        season.addTeam(2L);

        when(seasonRepository.findById(1L)).thenReturn(Optional.of(season));
        when(gameRepository.findConfirmedBySeasonId(1L)).thenReturn(List.of());
        when(teamRepository.findByIds(List.of(1L, 2L))).thenReturn(List.of(
                makeTeam(1L, "Anadolu Efes"),
                makeTeam(2L, "Barcelona")
        ));

        final List<StandingResponse> standings = standingService.calculateStandings(1L);

        assertThat(standings.get(0).rank()).isEqualTo(1);
        assertThat(standings.get(1).rank()).isEqualTo(2);
    }

    @Test
    @DisplayName("Sezona takım eklenmemişse boş liste döner")
    void returnsEmptyListWhenSeasonHasNoTeams() {
        final Season season = new Season("Empty", LocalDate.of(2025, 10, 1));

        when(seasonRepository.findById(1L)).thenReturn(Optional.of(season));
        when(gameRepository.findConfirmedBySeasonId(1L)).thenReturn(List.of());
        when(teamRepository.findByIds(List.of())).thenReturn(List.of());

        final List<StandingResponse> standings = standingService.calculateStandings(1L);

        assertThat(standings).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static Team makeTeam(long id, String name) {
        final Team t = new Team(name, "City");
        // Inject id via reflection since Team.id is auto-generated
        try {
            final var field = t.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(t, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return t;
    }
}

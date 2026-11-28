package com.basketballleague.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.basketballleague.dto.SeasonResponse;
import com.basketballleague.dto.StandingResponse;
import com.basketballleague.dto.TeamSeasonOverviewResponse;
import com.basketballleague.service.TeamSeasonService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamSeasonResourceTest {

    @Mock
    private TeamSeasonService teamSeasonService;

    @Test
    void listsTheSeasonsForATeam() {
        final List<SeasonResponse> seasons =
                List.of(new SeasonResponse(7L, "2026-27", LocalDate.of(2026, 10, 1), "ACTIVE"));
        when(teamSeasonService.listSeasons(1L)).thenReturn(seasons);

        assertThat(new TeamSeasonResource(teamSeasonService).listSeasons(1L)).isSameAs(seasons);
    }

    @Test
    void returnsTheRequestedTeamSeasonOverview() {
        final TeamSeasonOverviewResponse overview = new TeamSeasonOverviewResponse(
                new SeasonResponse(7L, "2026-27", LocalDate.of(2026, 10, 1), "ACTIVE"),
                new StandingResponse(1, 1L, "Anadolu Efes", 0, 0, 0, 0, 0, 0, 0), List.of());
        when(teamSeasonService.getOverview(1L, 7L)).thenReturn(overview);

        assertThat(new TeamSeasonResource(teamSeasonService).getOverview(1L, 7L)).isSameAs(overview);
    }

    @Test
    void rejectsNonPositiveTeamAndSeasonIds() throws NoSuchMethodException {
        final TeamSeasonResource target = new TeamSeasonResource(null);
        final Method listSeasons = TeamSeasonResource.class.getMethod("listSeasons", long.class);
        final Method getOverview = TeamSeasonResource.class.getMethod("getOverview", long.class, long.class);

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            final Validator validator = factory.getValidator();
            assertThat(validator.forExecutables().validateParameters(target, listSeasons, new Object[] {0L}))
                    .hasSize(1);
            assertThat(validator.forExecutables().validateParameters(target, getOverview, new Object[] {1L, 0L}))
                    .hasSize(1);
        }
    }
}

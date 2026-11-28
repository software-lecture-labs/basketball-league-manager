package com.basketballleague.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.basketballleague.dto.GameDetailResponse;
import com.basketballleague.dto.GameVersionRequest;
import com.basketballleague.dto.RecordPlayerPointsRequest;
import com.basketballleague.dto.RecordQuartersRequest;
import com.basketballleague.service.GameService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameResourceTest {

    private static final long GAME_ID = 10L;
    private static final long VERSION = 6L;
    private static final GameDetailResponse GAME = new GameDetailResponse(GAME_ID, 1L, 1, null, null, "FINISHED",
            84, 80, List.of(), List.of(), VERSION);

    @Mock
    private GameService gameService;

    private GameResource resource() {
        return new GameResource(gameService);
    }

    @Test
    void getGameReturnsTheGameFromTheService() {
        when(gameService.getGame(GAME_ID)).thenReturn(GAME);

        assertThat(resource().getGame(GAME_ID)).isSameAs(GAME);
    }

    @Test
    void transitionsPassTheBodyVersionToTheService() {
        final GameVersionRequest body = new GameVersionRequest(VERSION);
        when(gameService.start(GAME_ID, VERSION)).thenReturn(GAME);
        when(gameService.finish(GAME_ID, VERSION)).thenReturn(GAME);
        when(gameService.reopen(GAME_ID, VERSION)).thenReturn(GAME);
        when(gameService.confirm(GAME_ID, VERSION)).thenReturn(GAME);

        assertThat(resource().start(GAME_ID, body)).isSameAs(GAME);
        assertThat(resource().finish(GAME_ID, body)).isSameAs(GAME);
        assertThat(resource().reopen(GAME_ID, body)).isSameAs(GAME);
        assertThat(resource().confirm(GAME_ID, body)).isSameAs(GAME);
    }

    @Test
    void quarterAndStatsUpdatesPassTheWholeBody() {
        final RecordQuartersRequest quarters = new RecordQuartersRequest(VERSION, List.of());
        final RecordPlayerPointsRequest stats = new RecordPlayerPointsRequest(VERSION, List.of());
        when(gameService.recordQuarters(GAME_ID, quarters)).thenReturn(GAME);
        when(gameService.recordPlayerPoints(GAME_ID, stats)).thenReturn(GAME);

        assertThat(resource().recordQuarters(GAME_ID, quarters)).isSameAs(GAME);
        assertThat(resource().recordPlayerPoints(GAME_ID, stats)).isSameAs(GAME);
    }

    @Test
    void rejectsANonPositiveGameIdOrAMissingBody() throws NoSuchMethodException {
        final Method getGame = GameResource.class.getMethod("getGame", long.class);
        final Method confirm = GameResource.class.getMethod("confirm", long.class, GameVersionRequest.class);
        final Method quarters =
                GameResource.class.getMethod("recordQuarters", long.class, RecordQuartersRequest.class);
        final Method stats =
                GameResource.class.getMethod("recordPlayerPoints", long.class, RecordPlayerPointsRequest.class);

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            final Validator validator = factory.getValidator();
            final GameResource target = new GameResource(null);
            assertThat(validator.forExecutables().validateParameters(target, getGame, new Object[] {0L}))
                    .hasSize(1);
            assertThat(validator.forExecutables().validateParameters(target, confirm, new Object[] {1L, null}))
                    .hasSize(1);
            assertThat(validator.forExecutables().validateParameters(target, quarters, new Object[] {-1L, null}))
                    .hasSize(2);
            assertThat(validator.forExecutables().validateParameters(target, stats,
                    new Object[] {1L, new RecordPlayerPointsRequest(null, List.of())}))
                    .hasSize(1);
            assertThat(validator.forExecutables().validateParameters(target, confirm,
                    new Object[] {1L, new GameVersionRequest(0L)}))
                    .isEmpty();
        }
    }
}

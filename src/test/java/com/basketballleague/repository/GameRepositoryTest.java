package com.basketballleague.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.basketballleague.entity.Game;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameRepositoryTest {

    @Mock
    private EntityManager em;

    @Mock
    private TypedQuery<Game> gameQuery;

    private GameRepository repository;

    @BeforeEach
    void setUp() {
        repository = new GameRepository(em);
    }

    @Test
    void saveAllPersistsEveryGameAndFlushesOnceAtTheEnd() {
        final Game first = new Game(1L, 1, 10L, 20L);
        final Game second = new Game(1L, 1, 30L, 40L);

        final List<Game> saved = repository.saveAll(List.of(first, second));

        final InOrder order = inOrder(em);
        order.verify(em).persist(first);
        order.verify(em).persist(second);
        order.verify(em, times(1)).flush();
        assertThat(saved).containsExactly(first, second);
    }

    @Test
    void findBySeasonIdReturnsTheSeasonsGames() {
        final List<Game> games = List.of(new Game(5L, 1, 10L, 20L));
        when(em.createQuery(anyString(), eq(Game.class))).thenReturn(gameQuery);
        when(gameQuery.setParameter("seasonId", 5L)).thenReturn(gameQuery);
        when(gameQuery.getResultList()).thenReturn(games);

        assertThat(repository.findBySeasonId(5L)).isEqualTo(games);
    }

    @Test
    void findConfirmedBySeasonIdAsksOnlyForTheSeasonsConfirmedGames() {
        final List<Game> games = List.of(new Game(5L, 1, 10L, 20L));
        when(em.createQuery(contains("CONFIRMED"), eq(Game.class))).thenReturn(gameQuery);
        when(gameQuery.setParameter("seasonId", 5L)).thenReturn(gameQuery);
        when(gameQuery.getResultList()).thenReturn(games);

        assertThat(repository.findConfirmedBySeasonId(5L)).isEqualTo(games);
    }

    @Test
    void findRecentConfirmedBySeasonAndTeamLimitsAndOrdersTheTeamGames() {
        final List<Game> games = List.of(new Game(5L, 3, 10L, 20L));
        when(em.createQuery(contains("roundNumber DESC"), eq(Game.class))).thenReturn(gameQuery);
        when(gameQuery.setParameter("seasonId", 5L)).thenReturn(gameQuery);
        when(gameQuery.setParameter("teamId", 10L)).thenReturn(gameQuery);
        when(gameQuery.setMaxResults(5)).thenReturn(gameQuery);
        when(gameQuery.getResultList()).thenReturn(games);

        assertThat(repository.findRecentConfirmedBySeasonAndTeamId(5L, 10L, 5)).isEqualTo(games);
    }

    @Test
    void findByIdReturnsEmptyForAnUnknownGame() {
        when(em.find(Game.class, 99L)).thenReturn(null);

        assertThat(repository.findById(99L)).isEmpty();
    }

    @Test
    void findByIdReturnsTheGame() {
        final Game game = new Game(5L, 1, 10L, 20L);
        when(em.find(Game.class, 7L)).thenReturn(game);

        assertThat(repository.findById(7L)).containsSame(game);
    }

    @Test
    void forceVersionIncrementLocksTheGameThenFlushes() {
        final Game game = new Game(5L, 1, 10L, 20L);

        repository.forceVersionIncrement(game);

        final InOrder order = inOrder(em);
        order.verify(em).lock(game, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        order.verify(em).flush();
    }
}

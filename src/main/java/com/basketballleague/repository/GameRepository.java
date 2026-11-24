package com.basketballleague.repository;

import com.basketballleague.entity.Game;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Objects;

/**
 * Database access for {@link Game} entities via JPA.
 */
@ApplicationScoped
public class GameRepository {

    private final EntityManager em;

    /** Required by CDI to create the proxy; never used to serve calls. */
    protected GameRepository() {
        this.em = null;
    }

    @Inject
    public GameRepository(EntityManager em) {
        this.em = Objects.requireNonNull(em, "EntityManager must not be null");
    }

    /**
     * Inserts new games, such as a whole fixture, and flushes once at the end, so their generated ids are
     * available before the transaction commits. No query runs per game.
     */
    public List<Game> saveAll(List<Game> games) {
        Objects.requireNonNull(games, "Games must not be null");
        games.forEach(em::persist);
        em.flush();
        return games;
    }

    /**
     * The season's games ordered by round, then by id (the order they were generated in).
     */
    public List<Game> findBySeasonId(long seasonId) {
        return em.createQuery(
                        "SELECT g FROM Game g WHERE g.seasonId = :seasonId ORDER BY g.roundNumber ASC, g.id ASC",
                        Game.class)
                .setParameter("seasonId", seasonId)
                .getResultList();
    }
}

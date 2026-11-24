package com.basketballleague.repository;

import com.basketballleague.entity.Game;
import com.basketballleague.entity.GameStatus;
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

    public Game save(Game game) {
        Objects.requireNonNull(game, "Game must not be null");
        if (game.getId() == null) {
            em.persist(game);
            em.flush();
            return game;
        }
        return em.merge(game);
    }

    /**
     * Loads all CONFIRMED games for the given season.
     * Uses JOIN FETCH on homeTeam and awayTeam to prevent N+1 select queries.
     */
    public List<Game> findConfirmedGamesBySeason(Long seasonId) {
        Objects.requireNonNull(seasonId, "seasonId must not be null");
        return em.createQuery("""
                SELECT g FROM Game g
                JOIN FETCH g.homeTeam
                JOIN FETCH g.awayTeam
                WHERE g.season.id = :seasonId
                  AND g.status = :status
                """, Game.class)
                .setParameter("seasonId", seasonId)
                .setParameter("status", GameStatus.CONFIRMED)
                .getResultList();
    }
}

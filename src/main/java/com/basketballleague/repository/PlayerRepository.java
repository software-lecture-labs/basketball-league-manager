package com.basketballleague.repository;

import com.basketballleague.entity.Player;
import com.basketballleague.entity.Position;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Database access for {@link Player} entities via JPA.
 */
@ApplicationScoped
public class PlayerRepository {

    @PersistenceContext
    private EntityManager em;

    /** Required by CDI to create the proxy. */
    protected PlayerRepository() {
    }

    public PlayerRepository(EntityManager em) {
        this.em = Objects.requireNonNull(em, "EntityManager must not be null");
    }

    public Player save(Player player) {
        Objects.requireNonNull(player, "Player must not be null");
        if (player.getId() == null) {
            em.persist(player);
            return player;
        }
        return em.merge(player);
    }

    public Optional<Player> findById(Long id) {
        Objects.requireNonNull(id, "Player id must not be null");
        return Optional.ofNullable(em.find(Player.class, id));
    }

    public List<Player> findAll() {
        return em.createQuery(
                        "SELECT p FROM Player p ORDER BY p.lastName ASC, p.firstName ASC", Player.class)
                .getResultList();
    }

    public List<Player> findByTeamId(Long teamId) {
        Objects.requireNonNull(teamId, "Team id must not be null");
        return em.createQuery(
                        "SELECT p FROM Player p WHERE p.team.id = :teamId ORDER BY p.jerseyNumber ASC", Player.class)
                .setParameter("teamId", teamId)
                .getResultList();
    }

    public List<Player> findByPosition(Position position) {
        Objects.requireNonNull(position, "Position must not be null");
        return em.createQuery(
                        "SELECT p FROM Player p WHERE p.position = :position ORDER BY p.lastName ASC, p.firstName ASC",
                        Player.class)
                .setParameter("position", position)
                .getResultList();
    }

    public void delete(Player player) {
        Objects.requireNonNull(player, "Player must not be null");
        em.remove(em.contains(player) ? player : em.merge(player));
    }

    public long count() {
        return em.createQuery("SELECT COUNT(p) FROM Player p", Long.class)
                .getSingleResult();
    }
}

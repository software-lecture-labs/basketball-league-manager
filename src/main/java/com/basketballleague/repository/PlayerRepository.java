package com.basketballleague.repository;

import com.basketballleague.entity.Player;
import com.basketballleague.entity.Position;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Database access for {@link Player} entities via JPA.
 */
@ApplicationScoped
public class PlayerRepository {

    private static final String TEAM_ID = "teamId";

    private final EntityManager em;

    /** Required by CDI to create the proxy; never used to serve calls. */
    protected PlayerRepository() {
        this.em = null;
    }

    @Inject
    public PlayerRepository(EntityManager em) {
        this.em = Objects.requireNonNull(em, "EntityManager must not be null");
    }

    /**
     * Inserts a new player or merges an existing one. A new player is flushed at once, so its generated id is
     * available to the caller before the transaction commits (the identity column is filled by the INSERT).
     */
    public Player save(Player player) {
        Objects.requireNonNull(player, "Player must not be null");
        if (player.getId() == null) {
            em.persist(player);
            em.flush();
            return player;
        }
        return em.merge(player);
    }

    public Optional<Player> findById(Long id) {
        Objects.requireNonNull(id, "Player id must not be null");
        return Optional.ofNullable(em.find(Player.class, id));
    }

    /**
     * The players with the given ids in one query instead of one {@code find} per id. Unknown ids are skipped.
     */
    public List<Player> findByIds(Collection<Long> ids) {
        Objects.requireNonNull(ids, "Player ids must not be null");
        if (ids.isEmpty()) {
            return List.of();
        }
        return em.createQuery("SELECT p FROM Player p WHERE p.id IN :ids", Player.class)
                .setParameter("ids", ids)
                .getResultList();
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
                .setParameter(TEAM_ID, teamId)
                .getResultList();
    }

    /**
     * One page of a team's players ordered by jersey number, which is unique within a team.
     */
    public List<Player> findPageByTeamId(long teamId, int firstResult, int maxResults) {
        return em.createQuery(
                        "SELECT p FROM Player p WHERE p.team.id = :teamId ORDER BY p.jerseyNumber ASC", Player.class)
                .setParameter(TEAM_ID, teamId)
                .setFirstResult(firstResult)
                .setMaxResults(maxResults)
                .getResultList();
    }

    public long countByTeamId(long teamId) {
        return em.createQuery("SELECT COUNT(p) FROM Player p WHERE p.team.id = :teamId", Long.class)
                .setParameter(TEAM_ID, teamId)
                .getSingleResult();
    }

    public boolean existsByTeamIdAndJerseyNumber(long teamId, int jerseyNumber) {
        return em.createQuery(
                        "SELECT COUNT(p) FROM Player p WHERE p.team.id = :teamId AND p.jerseyNumber = :jerseyNumber",
                        Long.class)
                .setParameter(TEAM_ID, teamId)
                .setParameter("jerseyNumber", jerseyNumber)
                .getSingleResult() > 0;
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

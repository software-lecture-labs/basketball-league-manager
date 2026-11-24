package com.basketballleague.repository;

import com.basketballleague.entity.Team;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Database access for {@link Team} entities via JPA.
 */
@ApplicationScoped
public class TeamRepository {

    private final EntityManager em;

    /** Required by CDI to create the proxy; never used to serve calls. */
    protected TeamRepository() {
        this.em = null;
    }

    @Inject
    public TeamRepository(EntityManager em) {
        this.em = Objects.requireNonNull(em, "EntityManager must not be null");
    }

    /**
     * Inserts a new team or merges an existing one. A new team is flushed at once, so its generated id is available
     * to the caller before the transaction commits (the identity column is filled by the INSERT).
     */
    public Team save(Team team) {
        Objects.requireNonNull(team, "Team must not be null");
        if (team.getId() == null) {
            em.persist(team);
            em.flush();
            return team;
        }
        return em.merge(team);
    }

    public Optional<Team> findById(Long id) {
        Objects.requireNonNull(id, "Team id must not be null");
        return Optional.ofNullable(em.find(Team.class, id));
    }

    public Optional<Team> findByName(String name) {
        Objects.requireNonNull(name, "Team name must not be null");
        List<Team> results = em.createQuery(
                        "SELECT t FROM Team t WHERE t.name = :name", Team.class)
                .setParameter("name", name)
                .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    public List<Team> findAll() {
        return em.createQuery("SELECT t FROM Team t ORDER BY t.name ASC", Team.class)
                .getResultList();
    }

    /**
     * One page of teams ordered by name; the id breaks ties so pages never overlap.
     */
    public List<Team> findPage(int firstResult, int maxResults) {
        return em.createQuery("SELECT t FROM Team t ORDER BY t.name ASC, t.id ASC", Team.class)
                .setFirstResult(firstResult)
                .setMaxResults(maxResults)
                .getResultList();
    }

    /**
     * The teams with the given ids, ordered by name, in one query instead of one {@code find} per id.
     * Unknown ids are skipped.
     */
    public List<Team> findByIds(Collection<Long> ids) {
        Objects.requireNonNull(ids, "Team ids must not be null");
        if (ids.isEmpty()) {
            return List.of();
        }
        return em.createQuery("SELECT t FROM Team t WHERE t.id IN :ids ORDER BY t.name ASC, t.id ASC", Team.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    public void delete(Team team) {
        Objects.requireNonNull(team, "Team must not be null");
        em.remove(em.contains(team) ? team : em.merge(team));
    }

    public long count() {
        return em.createQuery("SELECT COUNT(t) FROM Team t", Long.class)
                .getSingleResult();
    }
}

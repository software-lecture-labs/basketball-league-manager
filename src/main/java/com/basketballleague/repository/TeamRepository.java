package com.basketballleague.repository;

import com.basketballleague.entity.Team;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Database access for {@link Team} entities via JPA.
 */
@ApplicationScoped
public class TeamRepository {

    @PersistenceContext
    private EntityManager em;

    /** Required by CDI to create the proxy. */
    protected TeamRepository() {
    }

    public TeamRepository(EntityManager em) {
        this.em = Objects.requireNonNull(em, "EntityManager must not be null");
    }

    public Team save(Team team) {
        Objects.requireNonNull(team, "Team must not be null");
        if (team.getId() == null) {
            em.persist(team);
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

    public void delete(Team team) {
        Objects.requireNonNull(team, "Team must not be null");
        em.remove(em.contains(team) ? team : em.merge(team));
    }

    public long count() {
        return em.createQuery("SELECT COUNT(t) FROM Team t", Long.class)
                .getSingleResult();
    }
}

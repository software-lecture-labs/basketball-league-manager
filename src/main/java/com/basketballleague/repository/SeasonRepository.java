package com.basketballleague.repository;

import com.basketballleague.entity.Season;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Database access for {@link Season} entities via JPA.
 */
@ApplicationScoped
public class SeasonRepository {

    private static final String SEASON_ID_MUST_NOT_BE_NULL = "Season id must not be null";

    private final EntityManager em;

    /** Required by CDI to create the proxy; never used to serve calls. */
    protected SeasonRepository() {
        this.em = null;
    }

    @Inject
    public SeasonRepository(EntityManager em) {
        this.em = Objects.requireNonNull(em, "EntityManager must not be null");
    }

    /**
     * Inserts a new season or merges an existing one. A new season is flushed at once, so its generated id is
     * available to the caller before the transaction commits (the identity column is filled by the INSERT).
     */
    public Season save(Season season) {
        Objects.requireNonNull(season, "Season must not be null");
        if (season.getId() == null) {
            em.persist(season);
            em.flush();
            return season;
        }
        return em.merge(season);
    }

    public Optional<Season> findById(Long id) {
        Objects.requireNonNull(id, SEASON_ID_MUST_NOT_BE_NULL);
        return Optional.ofNullable(em.find(Season.class, id));
    }

    /**
     * Loads the season and locks its row until the transaction ends ({@code SELECT ... FOR UPDATE}). Changes to the
     * team list and the season status use this, so two concurrent requests cannot both pass the rules of
     * {@link Season#addTeam} or {@link Season#activate}. Must be called inside a transaction.
     */
    public Optional<Season> findByIdForUpdate(Long id) {
        Objects.requireNonNull(id, SEASON_ID_MUST_NOT_BE_NULL);
        return Optional.ofNullable(em.find(Season.class, id, LockModeType.PESSIMISTIC_WRITE));
    }

    public Optional<Season> findByName(String name) {
        Objects.requireNonNull(name, "Season name must not be null");
        List<Season> results = em.createQuery("SELECT s FROM Season s WHERE s.name = :name", Season.class)
                .setParameter("name", name)
                .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    /**
     * One page of seasons, newest start date first; the id breaks ties so pages never overlap.
     */
    public List<Season> findPage(int firstResult, int maxResults) {
        return em.createQuery("SELECT s FROM Season s ORDER BY s.startDate DESC, s.id DESC", Season.class)
                .setFirstResult(firstResult)
                .setMaxResults(maxResults)
                .getResultList();
    }

    public long count() {
        return em.createQuery("SELECT COUNT(s) FROM Season s", Long.class)
                .getSingleResult();
    }

    /**
     * Ids of the season's teams in no particular order, read straight from {@code season_teams} in one query, so
     * callers outside a transaction do not depend on lazy loading of {@link Season#getTeamIds()}.
     */
    public List<Long> findTeamIds(long seasonId) {
        return em.createQuery("SELECT t FROM Season s JOIN s.teamIds t WHERE s.id = :seasonId", Long.class)
                .setParameter("seasonId", seasonId)
                .getResultList();
    }

    /**
     * Seasons that include the team, newest start date first.
     */
    public List<Season> findByTeamId(long teamId) {
        return em.createQuery("SELECT s FROM Season s JOIN s.teamIds seasonTeamId"
                        + " WHERE seasonTeamId = :teamId ORDER BY s.startDate DESC, s.id DESC", Season.class)
                .setParameter("teamId", teamId)
                .getResultList();
    }
}

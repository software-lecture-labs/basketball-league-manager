package com.basketballleague.repository;

import com.basketballleague.entity.Season;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Database access for {@link Season} entities via JPA.
 */
@ApplicationScoped
public class SeasonRepository {

    private final EntityManager em;

    /** Required by CDI to create the proxy; never used to serve calls. */
    protected SeasonRepository() {
        this.em = null;
    }

    @Inject
    public SeasonRepository(EntityManager em) {
        this.em = Objects.requireNonNull(em, "EntityManager must not be null");
    }

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
        Objects.requireNonNull(id, "Season id must not be null");
        return Optional.ofNullable(em.find(Season.class, id));
    }

    public List<Season> findAll() {
        return em.createQuery("SELECT s FROM Season s ORDER BY s.name ASC", Season.class)
                .getResultList();
    }
}

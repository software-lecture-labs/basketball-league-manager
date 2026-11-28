package com.basketballleague.repository;

import static com.basketballleague.support.EntityIds.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.basketballleague.entity.Season;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SeasonRepositoryTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 1);

    @Mock
    private EntityManager em;

    @Mock
    private TypedQuery<Season> seasonQuery;

    @Mock
    private TypedQuery<Long> longQuery;

    private SeasonRepository repository;

    @BeforeEach
    void setUp() {
        repository = new SeasonRepository(em);
    }

    private static Season season() {
        return new Season("2026-27", START);
    }

    @Test
    void savePersistsAndFlushesANewSeasonSoItsIdIsAvailable() {
        final Season season = season();

        final Season saved = repository.save(season);

        final InOrder order = inOrder(em);
        order.verify(em).persist(season);
        order.verify(em).flush();
        assertThat(saved).isSameAs(season);
    }

    @Test
    void saveMergesAnExistingSeason() {
        final Season season = withId(season(), 3L);
        when(em.merge(season)).thenReturn(season);

        assertThat(repository.save(season)).isSameAs(season);
    }

    @Test
    void findByIdReturnsEmptyForAnUnknownId() {
        when(em.find(Season.class, 9L)).thenReturn(null);

        assertThat(repository.findById(9L)).isEmpty();
    }

    @Test
    void findByIdForUpdateLocksTheSeasonRow() {
        final Season season = season();
        when(em.find(Season.class, 3L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(season);

        assertThat(repository.findByIdForUpdate(3L)).contains(season);
    }

    @Test
    void findByNameReturnsTheMatchingSeason() {
        final Season season = season();
        when(em.createQuery(anyString(), eq(Season.class))).thenReturn(seasonQuery);
        when(seasonQuery.setParameter("name", "2026-27")).thenReturn(seasonQuery);
        when(seasonQuery.getResultList()).thenReturn(List.of(season));

        assertThat(repository.findByName("2026-27")).contains(season);
    }

    @Test
    void findPageAsksForTheRequestedSlice() {
        final List<Season> seasons = List.of(season());
        when(em.createQuery(anyString(), eq(Season.class))).thenReturn(seasonQuery);
        when(seasonQuery.setFirstResult(20)).thenReturn(seasonQuery);
        when(seasonQuery.setMaxResults(10)).thenReturn(seasonQuery);
        when(seasonQuery.getResultList()).thenReturn(seasons);

        assertThat(repository.findPage(20, 10)).isEqualTo(seasons);
        verify(seasonQuery).setFirstResult(20);
        verify(seasonQuery).setMaxResults(10);
    }

    @Test
    void countReturnsTheNumberOfSeasons() {
        when(em.createQuery(anyString(), eq(Long.class))).thenReturn(longQuery);
        when(longQuery.getSingleResult()).thenReturn(4L);

        assertThat(repository.count()).isEqualTo(4L);
    }

    @Test
    void findTeamIdsReadsTheSeasonsTeamIdsInOneQuery() {
        when(em.createQuery(anyString(), eq(Long.class))).thenReturn(longQuery);
        when(longQuery.setParameter("seasonId", 3L)).thenReturn(longQuery);
        when(longQuery.getResultList()).thenReturn(List.of(1L, 4L));

        assertThat(repository.findTeamIds(3L)).containsExactly(1L, 4L);
    }

    @Test
    void findByTeamIdReturnsSeasonsContainingTheTeam() {
        final List<Season> seasons = List.of(season());
        when(em.createQuery(anyString(), eq(Season.class))).thenReturn(seasonQuery);
        when(seasonQuery.setParameter("teamId", 4L)).thenReturn(seasonQuery);
        when(seasonQuery.getResultList()).thenReturn(seasons);

        assertThat(repository.findByTeamId(4L)).isEqualTo(seasons);
    }
}

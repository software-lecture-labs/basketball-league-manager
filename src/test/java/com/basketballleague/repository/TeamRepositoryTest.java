package com.basketballleague.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.basketballleague.entity.Team;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamRepositoryTest {

    @Mock
    private EntityManager em;

    @Mock
    private TypedQuery<Team> teamQuery;

    @Mock
    private TypedQuery<Long> countQuery;

    private TeamRepository repository;

    @BeforeEach
    void setUp() {
        repository = new TeamRepository(em);
    }

    @Test
    @DisplayName("Persists new team when ID is null")
    void savesNewTeam() {
        Team team = new Team("Fenerbahçe Beko", "İstanbul");

        Team saved = repository.save(team);

        verify(em).persist(team);
        assertThat(saved).isSameAs(team);
    }

    @Test
    @DisplayName("Merges existing team when ID is present")
    void mergesExistingTeam() throws Exception {
        Team team = new Team("Panathinaikos", "Atina");
        java.lang.reflect.Field idField = Team.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(team, 10L);

        when(em.merge(team)).thenReturn(team);

        Team result = repository.save(team);

        verify(em).merge(team);
        assertThat(result).isSameAs(team);
    }

    @Test
    @DisplayName("Finds team by ID")
    void findsById() {
        Team team = new Team("Olympiacos", "Pireas");
        when(em.find(Team.class, 1L)).thenReturn(team);

        Optional<Team> found = repository.findById(1L);

        assertThat(found).contains(team);
    }

    @Test
    @DisplayName("Returns empty optional when team not found by ID")
    void returnsEmptyWhenNotFoundById() {
        when(em.find(Team.class, 999L)).thenReturn(null);

        Optional<Team> found = repository.findById(999L);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Finds team by name using JPQL")
    void findsByName() {
        Team team = new Team("Real Madrid", "Madrid");
        when(em.createQuery(anyString(), eq(Team.class))).thenReturn(teamQuery);
        when(teamQuery.setParameter(eq("name"), any())).thenReturn(teamQuery);
        when(teamQuery.getResultList()).thenReturn(List.of(team));

        Optional<Team> found = repository.findByName("Real Madrid");

        assertThat(found).contains(team);
    }

    @Test
    @DisplayName("Finds all teams ordered by name")
    void findsAllTeams() {
        List<Team> teams = List.of(
                new Team("Anadolu Efes", "İstanbul"),
                new Team("Fenerbahçe Beko", "İstanbul"));
        when(em.createQuery(anyString(), eq(Team.class))).thenReturn(teamQuery);
        when(teamQuery.getResultList()).thenReturn(teams);

        List<Team> result = repository.findAll();

        assertThat(result).hasSize(2).isEqualTo(teams);
    }

    @Test
    @DisplayName("Deletes team from persistence context")
    void deletesTeam() {
        Team team = new Team("AS Monaco", "Monaco");
        when(em.contains(team)).thenReturn(true);

        repository.delete(team);

        verify(em).remove(team);
    }

    @Test
    @DisplayName("Counts total number of teams")
    void countsTeams() {
        when(em.createQuery(anyString(), eq(Long.class))).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(20L);

        long count = repository.count();

        assertThat(count).isEqualTo(20L);
    }
}

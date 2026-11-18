package com.basketballleague.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.basketballleague.entity.Player;
import com.basketballleague.entity.Position;
import com.basketballleague.entity.Team;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlayerRepositoryTest {

    @Mock
    private EntityManager em;

    @Mock
    private TypedQuery<Player> playerQuery;

    @Mock
    private TypedQuery<Long> countQuery;

    private PlayerRepository repository;

    @BeforeEach
    void setUp() {
        repository = new PlayerRepository(em);
    }

    @Test
    @DisplayName("Persists new player when ID is null")
    void savesNewPlayer() {
        Player player = new Player("Tarik", "Biberovic", Position.SF, LocalDate.of(2001, 1, 28));

        Player saved = repository.save(player);

        verify(em).persist(player);
        assertThat(saved).isSameAs(player);
    }

    @Test
    @DisplayName("Merges existing player when ID is present")
    void mergesExistingPlayer() throws Exception {
        Player player = new Player("Kostas", "Sloukas", Position.PG, null);
        java.lang.reflect.Field idField = Player.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(player, 25L);

        when(em.merge(player)).thenReturn(player);

        Player result = repository.save(player);

        verify(em).merge(player);
        assertThat(result).isSameAs(player);
    }

    @Test
    @DisplayName("Finds player by ID")
    void findsById() {
        Player player = new Player("Kostas", "Sloukas", Position.PG, LocalDate.of(1990, 1, 15));
        when(em.find(Player.class, 5L)).thenReturn(player);

        Optional<Player> found = repository.findById(5L);

        assertThat(found).contains(player);
    }

    @Test
    @DisplayName("Finds all players")
    void findsAll() {
        List<Player> players = List.of(
                new Player("Facundo", "Campazzo", Position.PG, null),
                new Player("Walter", "Tavares", Position.C, null));
        when(em.createQuery(anyString(), eq(Player.class))).thenReturn(playerQuery);
        when(playerQuery.getResultList()).thenReturn(players);

        List<Player> result = repository.findAll();

        assertThat(result).hasSize(2).isEqualTo(players);
    }

    @Test
    @DisplayName("Finds players by team ID")
    void findsByTeamId() {
        Team team = new Team("Fenerbahçe Beko", "İstanbul");
        Player player = new Player(team, "Nigel", "Hayes-Davis", 11, Position.PF, null);

        when(em.createQuery(anyString(), eq(Player.class))).thenReturn(playerQuery);
        when(playerQuery.setParameter(eq("teamId"), any())).thenReturn(playerQuery);
        when(playerQuery.getResultList()).thenReturn(List.of(player));

        List<Player> result = repository.findByTeamId(1L);

        assertThat(result).containsExactly(player);
    }

    @Test
    @DisplayName("Finds free agent players")
    void findsFreeAgents() {
        Player freeAgent = new Player("John", "Wall", Position.PG, null);

        when(em.createQuery(anyString(), eq(Player.class))).thenReturn(playerQuery);
        when(playerQuery.getResultList()).thenReturn(List.of(freeAgent));

        List<Player> result = repository.findFreeAgents();

        assertThat(result).containsExactly(freeAgent);
    }

    @Test
    @DisplayName("Finds players by playing position")
    void findsByPosition() {
        Player center = new Player("Jan", "Vesely", Position.C, null);

        when(em.createQuery(anyString(), eq(Player.class))).thenReturn(playerQuery);
        when(playerQuery.setParameter(eq("position"), any())).thenReturn(playerQuery);
        when(playerQuery.getResultList()).thenReturn(List.of(center));

        List<Player> result = repository.findByPosition(Position.C);

        assertThat(result).containsExactly(center);
    }

    @Test
    @DisplayName("Deletes player from persistence context")
    void deletesPlayer() {
        Player player = new Player("Test", "Player", Position.SG, null);
        when(em.contains(player)).thenReturn(true);

        repository.delete(player);

        verify(em).remove(player);
    }

    @Test
    @DisplayName("Counts total number of players")
    void countsPlayers() {
        when(em.createQuery(anyString(), eq(Long.class))).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(150L);

        long count = repository.count();

        assertThat(count).isEqualTo(150L);
    }
}

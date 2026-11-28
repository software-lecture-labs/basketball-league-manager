package com.basketballleague.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.basketballleague.entity.Player;
import com.basketballleague.entity.Position;
import com.basketballleague.entity.Team;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InOrder;
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

    private final Team team = new Team("Fenerbahçe Beko", "İstanbul");

    private PlayerRepository repository;

    @BeforeEach
    void setUp() {
        repository = new PlayerRepository(em);
    }

    @Test
    @DisplayName("Persists and flushes new player when ID is null")
    void savesNewPlayer() {
        Player player = new Player(team, "Tarik", "Biberovic", 20, Position.SF);

        Player saved = repository.save(player);

        InOrder order = inOrder(em);
        order.verify(em).persist(player);
        order.verify(em).flush();
        assertThat(saved).isSameAs(player);
    }

    @Test
    @DisplayName("Merges existing player when ID is present")
    void mergesExistingPlayer() throws Exception {
        Player player = new Player(team, "Kostas", "Sloukas", 21, Position.PG);
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
        Player player = new Player(team, "Kostas", "Sloukas", 22, Position.PG);
        when(em.find(Player.class, 5L)).thenReturn(player);

        Optional<Player> found = repository.findById(5L);

        assertThat(found).contains(player);
    }

    @Test
    @DisplayName("Finds all players")
    void findsAll() {
        List<Player> players = List.of(
                new Player(team, "Facundo", "Campazzo", 23, Position.PG),
                new Player(team, "Walter", "Tavares", 24, Position.C));
        when(em.createQuery(anyString(), eq(Player.class))).thenReturn(playerQuery);
        when(playerQuery.getResultList()).thenReturn(players);

        List<Player> result = repository.findAll();

        assertThat(result).hasSize(2).isEqualTo(players);
    }

    @Test
    @DisplayName("Finds players by team ID")
    void findsByTeamId() {
        Player player = new Player(team, "Nigel", "Hayes-Davis", 11, Position.PF);

        when(em.createQuery(anyString(), eq(Player.class))).thenReturn(playerQuery);
        when(playerQuery.setParameter(eq("teamId"), any())).thenReturn(playerQuery);
        when(playerQuery.getResultList()).thenReturn(List.of(player));

        List<Player> result = repository.findByTeamId(1L);

        assertThat(result).containsExactly(player);
    }

    @Test
    @DisplayName("Finds one page of a team's players")
    void findsPageByTeamId() {
        Player player = new Player(team, "Shane", "Larkin", 3, Position.PG);
        when(em.createQuery(anyString(), eq(Player.class))).thenReturn(playerQuery);
        when(playerQuery.setParameter("teamId", 4L)).thenReturn(playerQuery);
        when(playerQuery.setFirstResult(10)).thenReturn(playerQuery);
        when(playerQuery.setMaxResults(5)).thenReturn(playerQuery);
        when(playerQuery.getResultList()).thenReturn(List.of(player));

        List<Player> result = repository.findPageByTeamId(4L, 10, 5);

        assertThat(result).containsExactly(player);
        verify(playerQuery).setFirstResult(10);
        verify(playerQuery).setMaxResults(5);
    }

    @Test
    @DisplayName("Counts a team's players")
    void countsByTeamId() {
        when(em.createQuery(anyString(), eq(Long.class))).thenReturn(countQuery);
        when(countQuery.setParameter("teamId", 4L)).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(12L);

        assertThat(repository.countByTeamId(4L)).isEqualTo(12L);
    }

    @ParameterizedTest
    @CsvSource({"0, false", "1, true"})
    @DisplayName("Reports whether a jersey number is taken in a team")
    void checksJerseyNumber(long matches, boolean expected) {
        when(em.createQuery(anyString(), eq(Long.class))).thenReturn(countQuery);
        when(countQuery.setParameter("teamId", 4L)).thenReturn(countQuery);
        when(countQuery.setParameter("jerseyNumber", 3)).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(matches);

        assertThat(repository.existsByTeamIdAndJerseyNumber(4L, 3)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Finds players by playing position")
    void findsByPosition() {
        Player center = new Player(team, "Jan", "Vesely", 25, Position.C);

        when(em.createQuery(anyString(), eq(Player.class))).thenReturn(playerQuery);
        when(playerQuery.setParameter(eq("position"), any())).thenReturn(playerQuery);
        when(playerQuery.getResultList()).thenReturn(List.of(center));

        List<Player> result = repository.findByPosition(Position.C);

        assertThat(result).containsExactly(center);
    }

    @Test
    @DisplayName("Deletes player from persistence context")
    void deletesPlayer() {
        Player player = new Player(team, "Test", "Player", 26, Position.SG);
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

    @Test
    @DisplayName("Loads players by id in one query")
    void findsPlayersByIds() {
        final List<Player> players = List.of(new Player(team, "Nigel", "Hayes-Davis", 11, Position.SF));
        when(em.createQuery(anyString(), eq(Player.class))).thenReturn(playerQuery);
        when(playerQuery.setParameter(eq("ids"), any())).thenReturn(playerQuery);
        when(playerQuery.getResultList()).thenReturn(players);

        assertThat(repository.findByIds(List.of(7L, 40L))).isEqualTo(players);
        verify(em).createQuery(anyString(), eq(Player.class));
    }

    @Test
    @DisplayName("Does not query the database for an empty id list")
    void findsNoPlayersForNoIds() {
        assertThat(repository.findByIds(List.of())).isEmpty();
        verifyNoInteractions(em);
    }
}

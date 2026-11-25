package com.basketballleague.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.basketballleague.domain.GameStatus;
import com.basketballleague.entity.Game;
import com.basketballleague.entity.Player;
import com.basketballleague.entity.Position;
import com.basketballleague.entity.Season;
import com.basketballleague.entity.Team;
import com.basketballleague.repository.GameRepository;
import com.basketballleague.repository.PlayerRepository;
import com.basketballleague.repository.SeasonRepository;
import com.basketballleague.repository.TeamRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies real JPQL queries, pessimistic locking, and optimistic lock conflict detection across separate
 * {@link EntityManager}s for all repositories against PostgreSQL 18 (issue #29, ADR-0004).
 */
class RepositoryIntegrationIT extends PostgresIntegrationSupport {

    @BeforeEach
    void resetDatabase() {
        resetToSeedState();
    }

    @Test
    void teamRepositoryExecutesQueriesOrderingAndCrudAgainstPostgres() {
        final long newTeamId = inTransaction(em -> {
            final TeamRepository teams = new TeamRepository(em);
            assertThat(teams.count()).isEqualTo(20L);
            assertThat(teams.findAll()).hasSize(20)
                    .extracting(Team::getName)
                    .contains("Anadolu Efes", "Fenerbahçe");
            assertThat(teams.findByName("Anadolu Efes")).isPresent();
            assertThat(teams.findByName("Bilinmeyen Kulüp")).isEmpty();
            assertThat(teams.findByIds(List.of())).isEmpty();

            final List<Team> page = teams.findPage(1, 5);
            assertThat(page).hasSize(5);

            final Team created = teams.save(new Team("Zirve Spor", "Bursa"));
            assertThat(created.getId()).isNotNull();
            return created.getId();
        });

        inTransactionVoid(em -> {
            final TeamRepository teams = new TeamRepository(em);
            final Team loaded = teams.findById(newTeamId).orElseThrow();
            loaded.rename("Zirve Basket");
            teams.save(loaded);
            em.flush();

            final long existingTeamId = teams.findByName("Anadolu Efes").orElseThrow().getId();
            assertThat(teams.findByIds(List.of(newTeamId, existingTeamId))).extracting(Team::getId)
                    .contains(newTeamId, existingTeamId);
            assertThat(teams.findByName("Zirve Basket")).isPresent();

            teams.delete(loaded);
        });

        readOnly(em -> {
            assertThat(new TeamRepository(em).findById(newTeamId)).isEmpty();
            return null;
        });
    }

    @Test
    void playerRepositoryExecutesRosterAndPositionQueries() {
        final long efesId = readOnly(em ->
                new TeamRepository(em).findByName("Anadolu Efes").orElseThrow().getId());

        readOnly(em -> {
            final PlayerRepository players = new PlayerRepository(em);
            final long rosterSize = players.countByTeamId(efesId);
            assertThat(rosterSize).isGreaterThan(0L);
            assertThat(players.findByTeamId(efesId)).hasSize((int) rosterSize)
                    .isSortedAccordingTo((a, b) -> Integer.compare(a.getJerseyNumber(), b.getJerseyNumber()));
            assertThat(players.findPageByTeamId(efesId, 0, 3)).hasSize(3);
            assertThat(players.existsByTeamIdAndJerseyNumber(efesId, 0)).isTrue();
            assertThat(players.existsByTeamIdAndJerseyNumber(efesId, 99)).isFalse();
            assertThat(players.findByPosition(Position.PG))
                    .isNotEmpty()
                    .allSatisfy(p -> assertThat(p.getPosition()).isEqualTo(Position.PG));
            assertThat(players.count()).isEqualTo(332L);
            return null;
        });

        final long createdPlayerId = inTransaction(em -> {
            final Team efes = new TeamRepository(em).findById(efesId).orElseThrow();
            final Player rookie = new Player(efes, "Deneme", "Oyuncu", 99, Position.C);
            return new PlayerRepository(em).save(rookie).getId();
        });

        inTransactionVoid(em -> {
            final PlayerRepository players = new PlayerRepository(em);
            assertThat(players.existsByTeamIdAndJerseyNumber(efesId, 99)).isTrue();
            players.delete(players.findById(createdPlayerId).orElseThrow());
        });

        readOnly(em -> {
            assertThat(new PlayerRepository(em).findById(createdPlayerId)).isEmpty();
            return null;
        });
    }

    @Test
    void seasonRepositoryQueriesPessimisticLockAndTeamIdsWorkTogether() {
        final List<Long> teamIds = readOnly(em ->
                new TeamRepository(em).findPage(0, 3).stream().map(Team::getId).toList());

        final long newerSeasonId = inTransaction(em -> {
            final SeasonRepository seasons = new SeasonRepository(em);
            final Season older = seasons.save(new Season("2025-26 Arşiv", LocalDate.of(2025, 10, 1)));
            older.addTeam(teamIds.get(0));
            final Season newer = seasons.save(new Season("2026-27 Güncel", LocalDate.of(2026, 10, 1)));
            newer.addTeam(teamIds.get(2));
            newer.addTeam(teamIds.get(0));
            return newer.getId();
        });

        inTransactionVoid(em -> {
            final SeasonRepository seasons = new SeasonRepository(em);
            final Season locked = seasons.findByIdForUpdate(newerSeasonId).orElseThrow();
            locked.addTeam(teamIds.get(1));
        });

        readOnly(em -> {
            final SeasonRepository seasons = new SeasonRepository(em);
            assertThat(seasons.count()).isEqualTo(2L);
            assertThat(seasons.findByName("2026-27 Güncel")).isPresent();
            assertThat(seasons.findPage(0, 10)).extracting(Season::getName)
                    .containsExactly("2026-27 Güncel", "2025-26 Arşiv");
            assertThat(seasons.findTeamIds(newerSeasonId))
                    .containsExactlyInAnyOrderElementsOf(teamIds);
            return null;
        });
    }

    @Test
    void gameRepositoryLoadsSeasonFixtureAndOnlyConfirmedGames() {
        final List<Long> teamIds = readOnly(em ->
                new TeamRepository(em).findPage(0, 4).stream().map(Team::getId).toList());

        final long[] ids = inTransaction(em -> {
            final SeasonRepository seasons = new SeasonRepository(em);
            final Season target = seasons.save(new Season("Hedef Sezon", LocalDate.of(2026, 10, 1)));
            final Season other = seasons.save(new Season("Diğer Sezon", LocalDate.of(2026, 10, 2)));

            final Game scheduled = new Game(target.getId(), 1, teamIds.get(0), teamIds.get(1));
            final Game confirmedTarget = new Game(target.getId(), 1, teamIds.get(2), teamIds.get(3));
            final Game roundTwo = new Game(target.getId(), 2, teamIds.get(1), teamIds.get(0));
            final Game confirmedOther = new Game(other.getId(), 1, teamIds.get(0), teamIds.get(1));

            new GameRepository(em).saveAll(List.of(roundTwo, scheduled, confirmedTarget, confirmedOther));

            em.createNativeQuery(
                            "UPDATE games SET status = 'CONFIRMED', home_score = 88, away_score = 81 "
                                    + "WHERE id IN (?1, ?2)")
                    .setParameter(1, confirmedTarget.getId())
                    .setParameter(2, confirmedOther.getId())
                    .executeUpdate();
            em.createNativeQuery(
                            "UPDATE games SET status = 'FINISHED', home_score = 75, away_score = 70 WHERE id = ?1")
                    .setParameter(1, roundTwo.getId())
                    .executeUpdate();

            return new long[]{target.getId(), scheduled.getId(), confirmedTarget.getId(), roundTwo.getId()};
        });

        readOnly(em -> {
            final GameRepository games = new GameRepository(em);
            assertThat(games.findBySeasonId(ids[0]))
                    .extracting(Game::getId)
                    .containsExactly(ids[1], ids[2], ids[3]);

            final List<Game> confirmed = games.findConfirmedBySeasonId(ids[0]);
            assertThat(confirmed).hasSize(1);
            assertThat(confirmed.getFirst().getId()).isEqualTo(ids[2]);
            assertThat(confirmed.getFirst().getStatus()).isEqualTo(GameStatus.CONFIRMED);
            return null;
        });
    }

    @Test
    void concurrentGameUpdateThrowsOptimisticLockExceptionAcrossTwoEntityManagers() {
        final List<Long> teamIds = readOnly(em ->
                new TeamRepository(em).findPage(0, 2).stream().map(Team::getId).toList());

        final long gameId = inTransaction(em -> {
            final Season season = new SeasonRepository(em)
                    .save(new Season("Çakışma Sezonu", LocalDate.of(2026, 10, 1)));
            final Game game = new Game(season.getId(), 1, teamIds.get(0), teamIds.get(1));
            new GameRepository(em).saveAll(List.of(game));
            return game.getId();
        });

        final EntityManager firstEm = emf().createEntityManager();
        final EntityManager secondEm = emf().createEntityManager();
        try {
            firstEm.getTransaction().begin();
            secondEm.getTransaction().begin();

            final Game firstCopy = firstEm.find(Game.class, gameId);
            final Game secondCopy = secondEm.find(Game.class, gameId);
            assertThat(firstCopy.getVersion()).isZero();
            assertThat(secondCopy.getVersion()).isZero();

            firstCopy.start();
            firstEm.getTransaction().commit();

            secondCopy.start();
            assertThatThrownBy(() -> secondEm.getTransaction().commit())
                    .isInstanceOf(RollbackException.class)
                    .hasCauseInstanceOf(OptimisticLockException.class);
        } finally {
            if (firstEm.getTransaction().isActive()) {
                firstEm.getTransaction().rollback();
            }
            if (secondEm.getTransaction().isActive()) {
                secondEm.getTransaction().rollback();
            }
            firstEm.close();
            secondEm.close();
        }

        readOnly(em -> {
            final Game persisted = em.find(Game.class, gameId);
            assertThat(persisted.getStatus()).isEqualTo(GameStatus.LIVE);
            assertThat(persisted.getVersion()).isEqualTo(1L);
            return null;
        });
    }
}

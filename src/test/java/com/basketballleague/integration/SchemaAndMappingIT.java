package com.basketballleague.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.basketballleague.domain.GameStatus;
import com.basketballleague.domain.SeasonStatus;
import com.basketballleague.entity.Game;
import com.basketballleague.entity.Player;
import com.basketballleague.entity.Position;
import com.basketballleague.entity.Season;
import com.basketballleague.entity.Team;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies Flyway migrations (V1–V5), PostgreSQL database constraints (including V4 result/stat tables), and
 * EclipseLink JPA entity mappings against a real PostgreSQL 18 container (issue #29).
 */
class SchemaAndMappingIT extends PostgresIntegrationSupport {

    private static final String SQLSTATE_FOREIGN_KEY_VIOLATION = "23503";
    private static final String SQLSTATE_UNIQUE_VIOLATION = "23505";
    private static final String SQLSTATE_CHECK_VIOLATION = "23514";

    @BeforeEach
    void resetDatabase() {
        resetToSeedState();
    }

    @Test
    void flywayAppliesAllMigrationsAndSeedsTeamsAndRosters() throws SQLException {
        try (Connection conn = openJdbcConnection(); Statement stmt = conn.createStatement()) {
            final List<String> versions = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank")) {
                while (rs.next()) {
                    assertThat(rs.getBoolean("success")).isTrue();
                    versions.add(rs.getString("version"));
                }
            }
            assertThat(versions).containsExactly("1", "2", "3", "4", "5");

            assertThat(scalarLong(conn, "SELECT COUNT(*) FROM teams")).isEqualTo(20L);
            assertThat(scalarLong(conn, "SELECT COUNT(*) FROM players")).isEqualTo(332L);
            assertThat(scalarLong(conn, "SELECT COUNT(*) FROM seasons")).isZero();
            assertThat(scalarLong(conn, "SELECT COUNT(*) FROM games")).isZero();
            assertThat(scalarLong(conn,
                    "SELECT COUNT(*) FROM teams t WHERE NOT EXISTS "
                            + "(SELECT 1 FROM players p WHERE p.team_id = t.id)"))
                    .isZero();
        }
    }

    @Test
    void v4SchemaColumnsTablesAndIndexesExist() throws SQLException {
        try (Connection conn = openJdbcConnection()) {
            assertColumnExists(conn, "games", "home_score", "integer", true);
            assertColumnExists(conn, "games", "away_score", "integer", true);

            assertColumnExists(conn, "game_quarter_scores", "game_id", "bigint", false);
            assertColumnExists(conn, "game_quarter_scores", "quarter_no", "integer", false);
            assertColumnExists(conn, "game_quarter_scores", "home_points", "integer", false);
            assertColumnExists(conn, "game_quarter_scores", "away_points", "integer", false);

            assertColumnExists(conn, "player_game_stats", "game_id", "bigint", false);
            assertColumnExists(conn, "player_game_stats", "player_id", "bigint", false);
            assertColumnExists(conn, "player_game_stats", "team_id", "bigint", false);
            assertColumnExists(conn, "player_game_stats", "points", "integer", false);
        }
    }

    @Test
    void v1AndV3ConstraintsEnforceDomainBoundsAtDatabaseLevel() throws SQLException {
        try (Connection conn = openJdbcConnection()) {
            final long firstTeamId = scalarLong(conn, "SELECT id FROM teams ORDER BY id LIMIT 1");
            final long secondTeamId = scalarLong(conn, "SELECT id FROM teams ORDER BY id OFFSET 1 LIMIT 1");
            assertSqlState(conn,
                    "INSERT INTO teams (name, city) VALUES ('Anadolu Efes', 'İstanbul')",
                    SQLSTATE_UNIQUE_VIOLATION);

            assertSqlState(conn,
                    "INSERT INTO players (team_id, first_name, last_name, jersey_number, position) "
                            + "VALUES (" + firstTeamId + ", 'Test', 'Oyuncu', 100, 'PG')",
                    SQLSTATE_CHECK_VIOLATION);

            assertSqlState(conn,
                    "INSERT INTO players (team_id, first_name, last_name, jersey_number, position) "
                            + "VALUES (" + firstTeamId + ", 'Test', 'Oyuncu', 50, 'XX')",
                    SQLSTATE_CHECK_VIOLATION);

            // ON DELETE RESTRICT keeps teams referenced by players
            assertSqlState(conn,
                    "DELETE FROM teams WHERE id = " + firstTeamId,
                    SQLSTATE_FOREIGN_KEY_VIOLATION);

            final long seasonId = insertSeason(conn, "Constraint Sezonu");
            assertSqlState(conn,
                    "INSERT INTO games (season_id, round_no, home_team_id, away_team_id, status) "
                            + "VALUES (" + seasonId + ", 1, " + firstTeamId + ", " + firstTeamId + ", 'SCHEDULED')",
                    SQLSTATE_CHECK_VIOLATION);
            assertSqlState(conn,
                    "INSERT INTO games (season_id, round_no, home_team_id, away_team_id, status) "
                            + "VALUES (" + seasonId + ", 0, " + firstTeamId + ", " + secondTeamId + ", 'SCHEDULED')",
                    SQLSTATE_CHECK_VIOLATION);
            assertSqlState(conn,
                    "INSERT INTO games (season_id, round_no, home_team_id, away_team_id, status) "
                            + "VALUES (" + seasonId + ", 1, " + firstTeamId + ", " + secondTeamId + ", 'UNKNOWN')",
                    SQLSTATE_CHECK_VIOLATION);
        }
    }

    @Test
    void v4ConstraintsEnforceScorePairingQuarterStatsAndForeignKeys() throws SQLException {
        try (Connection conn = openJdbcConnection()) {
            final long homeTeamId = scalarLong(conn, "SELECT id FROM teams ORDER BY id LIMIT 1");
            final long awayTeamId = scalarLong(conn, "SELECT id FROM teams ORDER BY id OFFSET 1 LIMIT 1");
            final long playerId = scalarLong(conn,
                    "SELECT id FROM players WHERE team_id = " + homeTeamId + " ORDER BY id LIMIT 1");
            final long seasonId = insertSeason(conn, "V4 Kısıt Sezonu");
            final long gameId = insertGame(conn, seasonId, 1, homeTeamId, awayTeamId);

            // games.home_score and away_score must be paired (both null or both non-null) and non-negative
            assertSqlState(conn,
                    "UPDATE games SET home_score = 80, away_score = NULL WHERE id = " + gameId,
                    SQLSTATE_CHECK_VIOLATION);
            assertSqlState(conn,
                    "UPDATE games SET home_score = NULL, away_score = 78 WHERE id = " + gameId,
                    SQLSTATE_CHECK_VIOLATION);
            assertSqlState(conn,
                    "UPDATE games SET home_score = -1, away_score = 78 WHERE id = " + gameId,
                    SQLSTATE_CHECK_VIOLATION);

            executeSql(conn, "UPDATE games SET home_score = 84, away_score = 78 WHERE id = " + gameId);

            // game_quarter_scores checks and PK
            executeSql(conn, "INSERT INTO game_quarter_scores (game_id, quarter_no, home_points, away_points) "
                    + "VALUES (" + gameId + ", 1, 22, 18)");
            assertSqlState(conn,
                    "INSERT INTO game_quarter_scores (game_id, quarter_no, home_points, away_points) "
                            + "VALUES (" + gameId + ", 1, 20, 20)",
                    SQLSTATE_UNIQUE_VIOLATION);
            assertSqlState(conn,
                    "INSERT INTO game_quarter_scores (game_id, quarter_no, home_points, away_points) "
                            + "VALUES (" + gameId + ", 0, 20, 20)",
                    SQLSTATE_CHECK_VIOLATION);
            assertSqlState(conn,
                    "INSERT INTO game_quarter_scores (game_id, quarter_no, home_points, away_points) "
                            + "VALUES (" + gameId + ", 2, -5, 20)",
                    SQLSTATE_CHECK_VIOLATION);

            // player_game_stats checks, PK, and RESTRICT FKs
            executeSql(conn, "INSERT INTO player_game_stats (game_id, player_id, team_id, points) "
                    + "VALUES (" + gameId + ", " + playerId + ", " + homeTeamId + ", 24)");
            assertSqlState(conn,
                    "INSERT INTO player_game_stats (game_id, player_id, team_id, points) "
                            + "VALUES (" + gameId + ", " + playerId + ", " + homeTeamId + ", 10)",
                    SQLSTATE_UNIQUE_VIOLATION);
            assertSqlState(conn,
                    "INSERT INTO player_game_stats (game_id, player_id, team_id, points) "
                            + "VALUES (" + gameId + ", " + playerId + ", " + homeTeamId + ", -1)",
                    SQLSTATE_CHECK_VIOLATION);
            assertSqlState(conn,
                    "DELETE FROM players WHERE id = " + playerId,
                    SQLSTATE_FOREIGN_KEY_VIOLATION);

            // Deleting the parent game cascades to quarter scores and player stats
            executeSql(conn, "DELETE FROM games WHERE id = " + gameId);
            assertThat(scalarLong(conn,
                    "SELECT COUNT(*) FROM game_quarter_scores WHERE game_id = " + gameId)).isZero();
            assertThat(scalarLong(conn,
                    "SELECT COUNT(*) FROM player_game_stats WHERE game_id = " + gameId)).isZero();
        }
    }

    @Test
    void jpaEntitiesPersistReloadAndMatchDatabaseSchema() {
        final long[] ids = inTransaction(em -> {
            final Team first = new Team("İstanbul Basket", "İstanbul");
            final Team second = new Team("Ankara Gücü", "Ankara");
            em.persist(first);
            em.persist(second);
            em.flush();

            final Player player = new Player(first, "Mert", "Kaya", 12, Position.PG);
            em.persist(player);
            player.transferTo(second, 24);
            em.flush();

            final Season season = new Season("2026-27 Test Ligi", LocalDate.of(2026, 10, 1));
            season.addTeam(first.getId());
            season.addTeam(second.getId());
            season.activate();
            em.persist(season);
            em.flush();

            final Game game = new Game(season.getId(), 1, first.getId(), second.getId());
            em.persist(game);
            em.flush();
            game.start();
            em.flush();

            return new long[]{first.getId(), second.getId(), player.getId(), season.getId(), game.getId()};
        });

        readOnly(em -> {
            final Team first = em.find(Team.class, ids[0]);
            final Team second = em.find(Team.class, ids[1]);
            final Player player = em.find(Player.class, ids[2]);
            final Season season = em.find(Season.class, ids[3]);
            final Game game = em.find(Game.class, ids[4]);

            assertThat(first.getName()).isEqualTo("İstanbul Basket");
            assertThat(player.getTeam()).isEqualTo(second);
            assertThat(player.getJerseyNumber()).isEqualTo(24);
            assertThat(player.getPosition()).isEqualTo(Position.PG);

            assertThat(season.getStatus()).isEqualTo(SeasonStatus.ACTIVE);
            assertThat(season.getTeamIds()).containsExactly(ids[0], ids[1]);

            assertThat(game.getSeasonId()).isEqualTo(ids[3]);
            assertThat(game.getRoundNumber()).isEqualTo(1);
            assertThat(game.getHomeTeamId()).isEqualTo(ids[0]);
            assertThat(game.getAwayTeamId()).isEqualTo(ids[1]);
            assertThat(game.getStatus()).isEqualTo(GameStatus.LIVE);
            assertThat(game.getVersion()).isEqualTo(1L);
            return null;
        });
    }

    private static long scalarLong(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            assertThat(rs.next()).isTrue();
            return rs.getLong(1);
        }
    }

    private static void assertColumnExists(Connection conn, String table, String column, String dataType,
            boolean nullable) throws SQLException {
        final String sql = "SELECT data_type, is_nullable FROM information_schema.columns "
                + "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).as("column %s.%s", table, column).isTrue();
                assertThat(rs.getString("data_type")).isEqualTo(dataType);
                assertThat(rs.getString("is_nullable")).isEqualTo(nullable ? "YES" : "NO");
            }
        }
    }

    private static void assertSqlState(Connection conn, String sql, String expectedSqlState) {
        assertThatThrownBy(() -> executeSql(conn, sql))
                .isInstanceOf(SQLException.class)
                .extracting(ex -> ((SQLException) ex).getSQLState())
                .isEqualTo(expectedSqlState);
    }

    private static void executeSql(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private static long insertSeason(Connection conn, String name) throws SQLException {
        final String sql = "INSERT INTO seasons (name, start_date, status) VALUES (?, '2026-10-01', 'ACTIVE') "
                + "RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private static long insertGame(Connection conn, long seasonId, int roundNo, long homeId, long awayId)
            throws SQLException {
        final String sql = "INSERT INTO games (season_id, round_no, home_team_id, away_team_id, status) "
                + "VALUES (?, ?, ?, ?, 'FINISHED') RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, seasonId);
            ps.setInt(2, roundNo);
            ps.setLong(3, homeId);
            ps.setLong(4, awayId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }
}

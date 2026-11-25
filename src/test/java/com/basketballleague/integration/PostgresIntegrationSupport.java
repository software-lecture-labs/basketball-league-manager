package com.basketballleague.integration;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Testcontainers PostgreSQL 18 + Flyway + EclipseLink {@code RESOURCE_LOCAL} harness for integration
 * tests (issues #29 and #28). Starts one container per test JVM and resets Flyway migrations between tests.
 */
abstract class PostgresIntegrationSupport {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:18-alpine");
    private static final String MIGRATIONS_LOCATION = "classpath:db/migration";
    private static final String TEST_PERSISTENCE_UNIT = "basketballTestPU";
    private static final Object INIT_LOCK = new Object();

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withDatabaseName("basketball_league_test")
            .withUsername("basketball")
            .withPassword("basketball");

    private static EntityManagerFactory entityManagerFactory;

    @BeforeAll
    static void startDatabaseAndPersistence() {
        synchronized (INIT_LOCK) {
            if (!POSTGRES.isRunning()) {
                POSTGRES.start();
            }
            if (entityManagerFactory == null || !entityManagerFactory.isOpen()) {
                resetToSeedState();
                entityManagerFactory = createEntityManagerFactory();
            }
        }
    }

    static void resetToSeedState() {
        final Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations(MIGRATIONS_LOCATION)
                .cleanDisabled(false)
                .load();
        flyway.clean();
        flyway.migrate();
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.getCache().evictAll();
        }
    }

    static EntityManagerFactory emf() {
        return entityManagerFactory;
    }

    static Connection openJdbcConnection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    static <T> T inTransaction(Function<EntityManager, T> work) {
        final EntityManager em = entityManagerFactory.createEntityManager();
        final EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            final T result = work.apply(em);
            tx.commit();
            return result;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    static void inTransactionVoid(Consumer<EntityManager> work) {
        inTransaction(em -> {
            work.accept(em);
            return null;
        });
    }

    static <T> T readOnly(Function<EntityManager, T> work) {
        final EntityManager em = entityManagerFactory.createEntityManager();
        try {
            return work.apply(em);
        } finally {
            em.close();
        }
    }

    private static EntityManagerFactory createEntityManagerFactory() {
        return Persistence.createEntityManagerFactory(TEST_PERSISTENCE_UNIT, Map.of(
                "jakarta.persistence.jdbc.driver", "org.postgresql.Driver",
                "jakarta.persistence.jdbc.url", POSTGRES.getJdbcUrl(),
                "jakarta.persistence.jdbc.user", POSTGRES.getUsername(),
                "jakarta.persistence.jdbc.password", POSTGRES.getPassword()));
    }
}

package com.basketballleague;

import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * PostgreSQL data source used by the {@code basketballPU} persistence unit.
 * Connection settings are resolved by Payara from the {@code DB_URL}, {@code DB_USER}
 * and {@code DB_PASSWORD} environment variables at deploy time.
 */
@DataSourceDefinition(
        name = "java:app/jdbc/basketballLeague",
        className = "org.postgresql.ds.PGSimpleDataSource",
        url = "${ENV=DB_URL}",
        user = "${ENV=DB_USER}",
        password = "${ENV=DB_PASSWORD}")
@ApplicationScoped
public class DataSourceConfig {
}

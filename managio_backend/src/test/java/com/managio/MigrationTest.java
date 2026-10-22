package com.managio;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/** The migrations build the documented schema on an empty database (docs/DATA_MODEL.md). */
class MigrationTest {

    @Test
    void migratesEmptyDatabase() throws Exception {
        try (var postgres = new PostgreSQLContainer("postgres:17-alpine")) {
            postgres.start();
            var result = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();

            assertThat(result.success).isTrue();
            try (Connection c = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery(
                         "select count(*) from information_schema.tables where table_schema = 'public' "
                                 + "and table_name <> 'flyway_schema_history'")) {
                rs.next();
                assertThat(rs.getInt(1)).isEqualTo(27);
            }
        }
    }
}

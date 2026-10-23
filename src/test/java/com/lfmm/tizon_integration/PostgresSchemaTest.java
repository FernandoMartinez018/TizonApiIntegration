package com.lfmm.tizon_integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F2-API-02: la API arranca contra un PostgreSQL 16 vacío: Flyway crea el esquema e
 * Hibernate lo valida (ddl-auto: validate). Se omite si no hay Docker.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PostgresSchemaTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired JdbcTemplate jdbc;

    @Test
    void flywayCreaElEsquemaYHibernateLoValida() {
        Integer applied = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success", Integer.class);
        assertThat(applied).isGreaterThanOrEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM scan_sessions", Integer.class)).isZero();
    }
}

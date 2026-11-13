package com.chapter.bookstore.integration;

import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/** Phase 1 wiring only: domain migrations and behavior arrive in subsequent phases. */
class PostgresInfrastructureIT extends PostgresTestSupport {
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired Environment environment;

    @Test
    void connectsToRealPostgres18WithFlywayAndJpaConfigured() {
        assertThat(jdbc.queryForObject("select current_database()", String.class))
                .isEqualTo("chapter_test");
        assertThat(jdbc.queryForObject("show server_version_num", Integer.class))
                .isBetween(180000, 189999);
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(entityManagerFactory.isOpen()).isTrue();
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(environment.getProperty("server.servlet.session.cookie.secure", Boolean.class))
                .isTrue();
        assertThat(environment.getProperty("server.servlet.session.cookie.http-only", Boolean.class))
                .isTrue();
        assertThat(environment.getProperty("server.servlet.session.timeout")).isEqualTo("30m");
    }
}

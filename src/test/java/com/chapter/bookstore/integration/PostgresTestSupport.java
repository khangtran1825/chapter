package com.chapter.bookstore.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** A single JVM-scoped database; Ryuk removes it on JVM exit. No Docker means failure. */
@SpringBootTest(properties = "spring.security.user.password=fictional-infrastructure-test-only")
public abstract class PostgresTestSupport {
  @ServiceConnection
  protected static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:18.7")
          .withDatabaseName("chapter_test")
          .withUsername("chapter_test")
          .withPassword("fictional-isolated-test-password");

  static {
    POSTGRES.start();
  }
}

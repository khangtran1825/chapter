package com.chapter.bookstore.checkout;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CheckoutLockRepository {
  private final JdbcTemplate jdbc;

  public CheckoutLockRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void lock(UUID key) {
    jdbc.execute("SET LOCAL lock_timeout = '3s'");
    jdbc.query(
        "select pg_advisory_xact_lock(?)",
        rs -> {
          return null;
        },
        key.getMostSignificantBits() ^ key.getLeastSignificantBits());
  }
}

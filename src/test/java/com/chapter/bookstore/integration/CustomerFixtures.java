package com.chapter.bookstore.integration;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class CustomerFixtures {
  public static final String PASSWORD = "fictional-reader-password-123";

  private CustomerFixtures() {}

  public static UUID create(
      JdbcTemplate jdbc, PasswordEncoder encoder, String email, boolean enabled) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "insert into"
            + " customer_account(id,full_name,email_normalized,password_hash,enabled,role,created_at)"
            + " values (?,'Bạn đọc mẫu',?,?,?,'CUSTOMER',now())",
        id,
        email,
        encoder.encode(PASSWORD),
        enabled);
    return id;
  }

  public static void reset(JdbcTemplate jdbc) {
    jdbc.execute("truncate customer_account cascade");
  }
}

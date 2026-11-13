package com.chapter.bookstore.integration;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.*;
import org.springframework.security.crypto.password.*;

class CustomerAdminIsolationIT extends PostgresTestSupport {
  @Autowired
  @Qualifier("customerPasswordEncoder")
  PasswordEncoder customerEncoder;

  @Autowired PasswordEncoder adminEncoder;

  @Test
  void encodersAreExplicitAndAdminHashesRemainCompatible() {
    String raw = "fictional-admin-secret-123";
    String hash = adminEncoder.encode(raw);
    assertThat(hash).startsWith("{bcrypt}");
    assertThat(adminEncoder.matches(raw, hash)).isTrue();
    String customer = customerEncoder.encode("😀".repeat(64));
    assertThat(customer).startsWith("{pbkdf2@SpringSecurity_v5_8}");
    assertThat(customerEncoder.matches("😀".repeat(64), customer)).isTrue();
  }
}

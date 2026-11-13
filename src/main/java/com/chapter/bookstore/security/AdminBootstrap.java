package com.chapter.bookstore.security;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap implements CommandLineRunner {
  private final AdminUserRepository users;
  private final PasswordEncoder encoder;
  private final String username, password;

  public AdminBootstrap(
      AdminUserRepository users,
      PasswordEncoder encoder,
      @Value("${CHAPTER_ADMIN_USERNAME:}") String username,
      @Value("${CHAPTER_ADMIN_PASSWORD:}") String password) {
    this.users = users;
    this.encoder = encoder;
    this.username = username.trim().toLowerCase(Locale.ROOT);
    this.password = password;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (username.isEmpty() && password.isEmpty()) return;
    if (!username.matches("[a-z0-9][a-z0-9._-]{2,99}")
        || password.length() < 12
        || password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new IllegalStateException(
          "Bootstrap admin requires a valid username and a 12+ character password of at most 72"
              + " UTF-8 bytes.");
    if (users.findByUsername(username).isPresent()) return;
    var u = new AdminUser();
    u.username = username;
    u.passwordHash = encoder.encode(password);
    users.save(u);
  }
}

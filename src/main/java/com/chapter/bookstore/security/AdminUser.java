package com.chapter.bookstore.security;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
public class AdminUser {
  @Id public UUID id = UUID.randomUUID();

  @Column(nullable = false, unique = true, length = 100)
  public String username;

  @Column(nullable = false, length = 255)
  public String passwordHash;

  @Column(nullable = false)
  public boolean enabled = true;

  @Column(nullable = false, length = 20)
  public String role = "ADMIN";
}

package com.chapter.bookstore.customer;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
public class CustomerAccount {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  public UUID id;

  @Column(nullable = false, length = 100)
  public String fullName;

  @Column(nullable = false, length = 254, unique = true)
  public String emailNormalized;

  @Column(nullable = false, length = 255)
  public String passwordHash;

  @Column(nullable = false)
  public boolean enabled = true;

  @Column(nullable = false, length = 20)
  public String role = "CUSTOMER";

  @Column(nullable = false)
  public Instant createdAt;
}

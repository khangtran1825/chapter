package com.chapter.bookstore.catalog;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
public class Category {
  @Id public UUID id = UUID.randomUUID();

  @Column(nullable = false, length = 100)
  public String name;

  @Column(nullable = false)
  public Instant createdAt = Instant.now();

  public Category() {}

  public Category(String name) {
    this.name = name;
  }
}

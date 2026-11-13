package com.chapter.bookstore.catalog;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
public class Book {
  @Id public UUID id = UUID.randomUUID();

  @Column(nullable = false, length = 255)
  public String title;

  @Column(nullable = false, length = 255)
  public String author;

  @Column(nullable = false, length = 10000)
  public String description;

  @Column(length = 255)
  public String coverPath;

  @Column(nullable = false)
  public long priceVnd;

  @Column(nullable = false)
  public int stock;

  @Column(nullable = false)
  public boolean active = true;

  @Version public long version;

  @Column(nullable = false)
  public Instant createdAt = Instant.now();

  @Column(nullable = false)
  public Instant updatedAt = Instant.now();

  @ManyToOne(optional = false)
  @JoinColumn(name = "category_id")
  public Category category;

  @PreUpdate
  void updated() {
    updatedAt = Instant.now();
  }

  public String getCover() {
    return com.chapter.bookstore.shared.Presentation.resolveCover(coverPath);
  }
}

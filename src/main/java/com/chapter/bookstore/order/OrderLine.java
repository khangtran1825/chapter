package com.chapter.bookstore.order;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
public class OrderLine {
  @Id public UUID id = UUID.randomUUID();

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id")
  public CustomerOrder order;

  @Column(nullable = false)
  public UUID bookId;

  @Column(nullable = false, length = 255)
  public String titleSnapshot;

  @Column(nullable = false, length = 255)
  public String authorSnapshot;

  @Column(nullable = false)
  public long unitPriceVnd;

  @Column(nullable = false)
  public int quantity;

  @Column(nullable = false)
  public long lineTotalVnd;
}

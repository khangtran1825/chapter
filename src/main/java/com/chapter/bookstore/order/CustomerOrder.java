package com.chapter.bookstore.order;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "customer_order")
public class CustomerOrder {
  @Id public UUID id = UUID.randomUUID();

  @Column(nullable = false, length = 40, unique = true)
  public String reference;

  @Column(nullable = false, unique = true)
  public UUID checkoutKey;

  @Column(nullable = false, length = 64)
  public String requestFingerprint;

  @Column(nullable = false, length = 64)
  public String ownerTokenHash;

  @Column(nullable = false, length = 100)
  public String recipientName;

  @Column(nullable = false, length = 10)
  public String phone;

  @Column(nullable = false, length = 255)
  public String street;

  @Column(nullable = false, length = 100)
  public String ward;

  @Column(nullable = false, length = 100)
  public String district;

  @Column(nullable = false, length = 100)
  public String province;

  @Column(nullable = false)
  public long subtotalVnd;

  @Column(nullable = false)
  public long deliveryVnd;

  @Column(nullable = false)
  public long totalVnd;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  public OrderStatus status = OrderStatus.PENDING;

  @Column(nullable = false)
  public Instant createdAt = Instant.now();

  @Column(nullable = false)
  public Instant updatedAt = Instant.now();

  @Version public long version;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @OrderBy("id")
  public List<OrderLine> lines = new ArrayList<>();

  @PreUpdate
  void updated() {
    updatedAt = Instant.now();
  }
}

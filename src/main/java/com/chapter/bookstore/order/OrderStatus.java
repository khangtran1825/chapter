package com.chapter.bookstore.order;

public enum OrderStatus {
  PENDING("Chờ xác nhận"),
  CONFIRMED("Đã xác nhận"),
  SHIPPING("Đang giao"),
  COMPLETED("Hoàn tất"),
  CANCELLED("Đã hủy");
  public final String label;

  OrderStatus(String label) {
    this.label = label;
  }

  public boolean allows(OrderStatus next) {
    return this == next
        || (this == PENDING && (next == CONFIRMED || next == CANCELLED))
        || (this == CONFIRMED && (next == SHIPPING || next == CANCELLED))
        || (this == SHIPPING && next == COMPLETED);
  }
}

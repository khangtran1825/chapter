package com.chapter.bookstore.shared;

public final class Money {
  private Money() {}

  public static long add(long a, long b) {
    if (a < 0 || b < 0) throw new IllegalArgumentException("Số tiền không hợp lệ");
    try {
      return Math.addExact(a, b);
    } catch (ArithmeticException ex) {
      throw new IllegalArgumentException("Tổng tiền vượt giới hạn");
    }
  }

  public static long multiply(long price, int quantity) {
    if (price < 0 || quantity < 0)
      throw new IllegalArgumentException("Số tiền hoặc số lượng không hợp lệ");
    try {
      return Math.multiplyExact(price, quantity);
    } catch (ArithmeticException ex) {
      throw new IllegalArgumentException("Tổng tiền vượt giới hạn");
    }
  }
}

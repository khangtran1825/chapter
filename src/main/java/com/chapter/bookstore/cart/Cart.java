package com.chapter.bookstore.cart;

import com.chapter.bookstore.shared.WebProblem;
import java.util.*;

public class Cart implements java.io.Serializable {
  private final Map<UUID, Integer> items = new LinkedHashMap<>();
  private long version;

  public synchronized Map<UUID, Integer> snapshot() {
    return new LinkedHashMap<>(items);
  }

  public synchronized long version() {
    return version;
  }

  public synchronized void add(UUID id, int quantity, int stock) {
    if (quantity < 1) throw new WebProblem(422, "Số lượng phải là số nguyên dương.");
    int next;
    try {
      next = Math.addExact(items.getOrDefault(id, 0), quantity);
    } catch (ArithmeticException ex) {
      throw new WebProblem(422, "Số lượng vượt giới hạn.");
    }
    set(id, next, stock);
  }

  public synchronized void set(UUID id, int quantity, int stock) {
    if (quantity < 1 || quantity > stock)
      throw new WebProblem(422, "Số lượng phải từ 1 đến số sách còn trong kho.");
    if (!items.containsKey(id) && items.size() >= 100)
      throw new WebProblem(422, "Giỏ sách tối đa 100 đầu sách.");
    items.put(id, quantity);
    version++;
  }

  public synchronized void remove(UUID id) {
    if (items.remove(id) != null) version++;
  }

  public synchronized void clearIfVersion(long expected) {
    if (version == expected) {
      items.clear();
      version++;
    }
  }
}

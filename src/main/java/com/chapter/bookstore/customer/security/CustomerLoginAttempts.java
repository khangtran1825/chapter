package com.chapter.bookstore.customer.security;

import java.time.*;
import java.util.*;

public class CustomerLoginAttempts {
  private static final Duration WINDOW = Duration.ofMinutes(5);

  private static class Attempt {
    Deque<Instant> failures = new ArrayDeque<>();
    Instant blocked, touched;
  }

  private final Map<String, Attempt> entries = new HashMap<>();
  private final Clock clock;
  private final int capacity;

  public CustomerLoginAttempts(Clock clock, int capacity) {
    this.clock = clock;
    this.capacity = capacity;
  }

  public record Decision(int status, long retryAfter) {}

  private void refresh(Attempt a, Instant now) {
    if (a.blocked != null && !now.isBefore(a.blocked)) {
      a.blocked = null;
      a.failures.clear();
    }
    while (!a.failures.isEmpty() && !a.failures.peek().isAfter(now.minus(WINDOW)))
      a.failures.remove();
  }

  public synchronized Decision permit(String key) {
    Instant now = clock.instant();
    Attempt a = entries.get(key);
    if (a != null) {
      refresh(a, now);
      if (a.blocked != null)
        return new Decision(
            429, Math.max(1, (Duration.between(now, a.blocked).toMillis() + 999) / 1000));
    } else {
      entries
          .values()
          .removeIf(
              v -> {
                refresh(v, now);
                return v.blocked == null
                    && v.failures.isEmpty()
                    && !v.touched.isAfter(now.minus(WINDOW));
              });
      if (entries.size() >= capacity) return new Decision(503, 300);
      a = new Attempt();
      entries.put(key, a);
    }
    a.touched = now;
    return new Decision(0, 0);
  }

  public synchronized void failed(String key) {
    Attempt a = entries.get(key);
    if (a == null) return;
    Instant now = clock.instant();
    refresh(a, now);
    if (a.blocked != null) return;
    a.failures.add(now);
    a.touched = now;
    if (a.failures.size() >= 5) a.blocked = now.plus(WINDOW);
  }

  public synchronized void succeeded(String key) {
    entries.remove(key);
  }

  public synchronized int size() {
    return entries.size();
  }
}

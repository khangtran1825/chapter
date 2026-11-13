package com.chapter.bookstore.support;

import java.time.*;
import java.util.concurrent.atomic.AtomicReference;

public final class MutableClock extends Clock {
  private final AtomicReference<Instant> now;

  public MutableClock(Instant initial) {
    now = new AtomicReference<>(initial);
  }

  public void advance(Duration duration) {
    now.updateAndGet(i -> i.plus(duration));
  }

  @Override
  public ZoneId getZone() {
    return ZoneOffset.UTC;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    return this;
  }

  @Override
  public Instant instant() {
    return now.get();
  }
}

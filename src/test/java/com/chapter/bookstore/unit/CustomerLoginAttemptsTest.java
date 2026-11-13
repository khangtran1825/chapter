package com.chapter.bookstore.unit;

import static org.assertj.core.api.Assertions.*;

import com.chapter.bookstore.customer.security.*;
import com.chapter.bookstore.support.MutableClock;
import java.time.*;
import org.junit.jupiter.api.Test;

class CustomerLoginAttemptsTest {
  @Test
  void blockStartsAtFifthAndDoesNotExtend() {
    var clock = new MutableClock(Instant.EPOCH);
    var a = new CustomerLoginAttempts(clock, 10);
    for (int i = 0; i < 5; i++) {
      assertThat(a.permit("a").status()).isZero();
      a.failed("a");
      clock.advance(Duration.ofSeconds(10));
    }
    assertThat(a.permit("a").status()).isEqualTo(429);
    clock.advance(Duration.ofSeconds(290));
    assertThat(a.permit("a").status()).isZero();
  }

  @Test
  void slidingWindowSuccessAndIndependentSources() {
    var c = new MutableClock(Instant.EPOCH);
    var a = new CustomerLoginAttempts(c, 10);
    a.permit("a");
    for (int i = 0; i < 4; i++) a.failed("a");
    c.advance(Duration.ofMinutes(5));
    a.failed("a");
    assertThat(a.permit("a").status()).isZero();
    a.succeeded("a");
    assertThat(a.size()).isZero();
    assertThat(a.permit("b").status()).isZero();
  }

  @Test
  void capacityPreservesBlockedKeysAndExpires() {
    var c = new MutableClock(Instant.EPOCH);
    var a = new CustomerLoginAttempts(c, 1);
    a.permit("a");
    for (int i = 0; i < 5; i++) a.failed("a");
    assertThat(a.permit("b").status()).isEqualTo(503);
    assertThat(a.permit("a").status()).isEqualTo(429);
    c.advance(Duration.ofMinutes(5));
    assertThat(a.permit("b").status()).isZero();
    assertThat(a.size()).isEqualTo(1);
  }
}

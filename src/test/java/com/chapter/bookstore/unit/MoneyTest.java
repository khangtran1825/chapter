package com.chapter.bookstore.unit;

import static org.assertj.core.api.Assertions.*;

import com.chapter.bookstore.shared.Money;
import org.junit.jupiter.api.Test;

class MoneyTest {
  @Test
  void calculatesWholeDong() {
    assertThat(Money.multiply(125000, 3)).isEqualTo(375000);
  }

  @Test
  void rejectsOverflow() {
    assertThatThrownBy(() -> Money.add(Long.MAX_VALUE, 1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNegativeInputs() {
    assertThatThrownBy(() -> Money.multiply(-1, 2)).isInstanceOf(IllegalArgumentException.class);
  }
}

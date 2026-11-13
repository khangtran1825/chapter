package com.chapter.bookstore.unit;

import static org.assertj.core.api.Assertions.*;

import com.chapter.bookstore.cart.Cart;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CartTest {
  @Test
  void combinesUpdatesAndRemovesWithoutExposingMutableState() {
    var c = new Cart();
    var id = UUID.randomUUID();
    c.add(id, 1, 3);
    c.add(id, 1, 3);
    assertThat(c.snapshot().get(id)).isEqualTo(2);
    c.snapshot().clear();
    assertThat(c.snapshot()).hasSize(1);
    c.set(id, 3, 3);
    c.remove(id);
    assertThat(c.snapshot()).isEmpty();
  }

  @Test
  void rejectsInvalidAndOverflowQuantities() {
    var c = new Cart();
    var id = UUID.randomUUID();
    assertThatThrownBy(() -> c.add(id, 0, 3)).isInstanceOf(RuntimeException.class);
    assertThatThrownBy(() -> c.add(id, 4, 3)).isInstanceOf(RuntimeException.class);
    c.add(id, Integer.MAX_VALUE, Integer.MAX_VALUE);
    assertThatThrownBy(() -> c.add(id, 1, Integer.MAX_VALUE)).isInstanceOf(RuntimeException.class);
  }

  @Test
  void limitsLinesAndDoesNotClearNewerCart() {
    var c = new Cart();
    for (int i = 0; i < 100; i++) c.add(UUID.randomUUID(), 1, 1);
    assertThatThrownBy(() -> c.add(UUID.randomUUID(), 1, 1)).isInstanceOf(RuntimeException.class);
    long old = c.version();
    c.remove(c.snapshot().keySet().iterator().next());
    c.clearIfVersion(old);
    assertThat(c.snapshot()).hasSize(99);
  }
}

package com.chapter.bookstore.checkout;

import java.util.*;

public record CheckoutQuote(UUID key, long cartVersion, Map<UUID, Line> lines)
    implements java.io.Serializable {
  public record Line(UUID id, String title, long price, int quantity, int stock)
      implements java.io.Serializable {}

  public CheckoutQuote {
    lines = Map.copyOf(lines);
  }
}

package com.chapter.bookstore.cart;

import com.chapter.bookstore.catalog.*;
import com.chapter.bookstore.shared.Money;
import jakarta.servlet.http.HttpSession;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {
  private final BookRepository books;

  public CartService(BookRepository books) {
    this.books = books;
  }

  public static Cart cart(HttpSession session) {
    synchronized (session) {
      var c = (Cart) session.getAttribute("cart");
      if (c == null) {
        c = new Cart();
        session.setAttribute("cart", c);
      }
      return c;
    }
  }

  public record Line(
      UUID id,
      String title,
      long price,
      int quantity,
      long subtotal,
      int stock,
      boolean available,
      String warning) {}

  public record View(List<Line> lines, long total, boolean canCheckout) {}

  @Transactional(readOnly = true)
  public View view(Cart cart) {
    List<Line> lines = new ArrayList<>();
    long total = 0;
    boolean valid = true;
    for (var e : cart.snapshot().entrySet()) {
      Book b = books.findById(e.getKey()).orElse(null);
      boolean available = b != null && b.active && b.stock >= e.getValue();
      long price = b == null ? 0 : b.priceVnd;
      long subtotal = Money.multiply(price, e.getValue());
      total = Money.add(total, subtotal);
      lines.add(
          new Line(
              e.getKey(),
              b == null ? "Sách không còn tồn tại" : b.title,
              price,
              e.getValue(),
              subtotal,
              b == null ? 0 : b.stock,
              available,
              available ? "" : "Sách đã thay đổi hoặc không đủ số lượng. Vui lòng cập nhật giỏ."));
      valid &= available;
    }
    return new View(lines, total, valid && !lines.isEmpty());
  }
}

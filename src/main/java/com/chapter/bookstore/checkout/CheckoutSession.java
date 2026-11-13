package com.chapter.bookstore.checkout;

import com.chapter.bookstore.shared.WebProblem;
import jakarta.servlet.http.HttpSession;
import java.util.*;

public class CheckoutSession implements java.io.Serializable {
  public final String ownerToken = UUID.randomUUID().toString() + UUID.randomUUID();
  private final LinkedHashMap<UUID, CheckoutQuote> quotes = new LinkedHashMap<>();

  public static CheckoutSession get(HttpSession session) {
    synchronized (session) {
      var s = (CheckoutSession) session.getAttribute("checkout");
      if (s == null) {
        s = new CheckoutSession();
        session.setAttribute("checkout", s);
      }
      return s;
    }
  }

  public synchronized void remember(CheckoutQuote quote) {
    quotes.put(quote.key(), quote);
    while (quotes.size() > 32) quotes.remove(quotes.keySet().iterator().next());
  }

  public synchronized CheckoutQuote quote(UUID key) {
    var q = quotes.get(key);
    if (q == null)
      throw new WebProblem(409, "Phiên thanh toán đã hết hạn. Vui lòng xem lại giỏ sách.");
    return q;
  }
}

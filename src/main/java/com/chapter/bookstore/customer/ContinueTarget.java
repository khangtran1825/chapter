package com.chapter.bookstore.customer;

import jakarta.servlet.http.HttpSession;

public enum ContinueTarget {
  BOOKS("books", "/books"),
  CART("cart", "/cart"),
  CHECKOUT("checkout", "/checkout"),
  ACCOUNT("account", "/account");
  public final String token, path;

  ContinueTarget(String token, String path) {
    this.token = token;
    this.path = path;
  }

  public static ContinueTarget parse(String token) {
    for (var t : values()) if (t.token.equals(token)) return t;
    return BOOKS;
  }

  public static void remember(HttpSession s, String token) {
    if (token != null) s.setAttribute("customerContinueTarget", parse(token));
  }

  public static ContinueTarget current(HttpSession s) {
    return s.getAttribute("customerContinueTarget") instanceof ContinueTarget t ? t : BOOKS;
  }

  public static String consume(HttpSession s) {
    var t = current(s);
    s.removeAttribute("customerContinueTarget");
    return t.path;
  }
}

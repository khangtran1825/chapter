package com.chapter.bookstore.customer;

public final class CustomerPasswordPolicy {
  private CustomerPasswordPolicy() {}

  public static boolean valid(String password) {
    if (password == null) return false;
    int n = password.codePointCount(0, password.length());
    return n >= 12 && n <= 64;
  }
}

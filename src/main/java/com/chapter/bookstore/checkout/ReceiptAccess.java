package com.chapter.bookstore.checkout;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;

public final class ReceiptAccess {
  private ReceiptAccess() {}

  public static String hash(String text) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public static boolean owns(String hash, String token) {
    return MessageDigest.isEqual(
        hash.getBytes(StandardCharsets.UTF_8), hash(token).getBytes(StandardCharsets.UTF_8));
  }
}

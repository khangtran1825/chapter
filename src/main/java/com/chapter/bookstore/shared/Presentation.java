package com.chapter.bookstore.shared;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.stereotype.Component;

@Component("present")
public class Presentation {
  public String money(long amount) {
    return String.format(Locale.forLanguageTag("vi-VN"), "%,d ₫", amount);
  }

  public static String resolveCover(String path) {
    return path != null
            && path.matches("/images/[a-zA-Z0-9._-]+\\.(svg|png|jpg|jpeg|webp)")
            && new org.springframework.core.io.ClassPathResource("static" + path).exists()
        ? path
        : "/images/book-placeholder.svg";
  }

  public String date(Instant instant) {
    return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        .withZone(ZoneId.of("Asia/Ho_Chi_Minh"))
        .format(instant);
  }

  public static UUID id(String value) {
    try {
      return UUID.fromString(value);
    } catch (RuntimeException ex) {
      throw new WebProblem(400, "Mã không hợp lệ.");
    }
  }
}

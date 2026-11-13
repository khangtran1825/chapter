package com.chapter.bookstore.customer;

import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class CustomerIdentityPolicy {
  private final Validator validator;

  public CustomerIdentityPolicy(Validator validator) {
    this.validator = validator;
  }

  public static String normalizeEmail(String input) {
    return input == null ? "" : input.strip().toLowerCase(Locale.ROOT);
  }

  public static String normalizeName(String input) {
    return input == null ? "" : input.strip();
  }

  public boolean validEmail(String input) {
    return input != null
        && input.codePointCount(0, input.length()) <= 254
        && validator.validateValue(EmailValue.class, "value", input).isEmpty();
  }

  public static boolean validName(String input) {
    String name = normalizeName(input);
    int n = name.codePointCount(0, name.length());
    return n >= 1 && n <= 100;
  }

  public static class EmailValue {
    @NotBlank @Email public String value;
  }
}

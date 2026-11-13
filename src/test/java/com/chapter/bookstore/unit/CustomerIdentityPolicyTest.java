package com.chapter.bookstore.unit;

import static org.assertj.core.api.Assertions.*;

import com.chapter.bookstore.customer.CustomerIdentityPolicy;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

class CustomerIdentityPolicyTest {
  @Test
  void canonicalEmailKeepsDotsAndTags() {
    assertThat(CustomerIdentityPolicy.normalizeEmail("  Reader.Name+Tag@EXAMPLE.COM  "))
        .isEqualTo("reader.name+tag@example.com");
  }

  @Test
  void invalidEmailAndNameAreRejected() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var p = new CustomerIdentityPolicy(factory.getValidator());
      assertThat(p.validEmail("bad")).isFalse();
      assertThat(p.validEmail("a".repeat(250) + "@example.com")).isFalse();
      assertThat(p.validEmail("name@example.com")).isTrue();
    }
    assertThat(CustomerIdentityPolicy.validName(" ")).isFalse();
    assertThat(CustomerIdentityPolicy.validName("😀".repeat(100))).isTrue();
    assertThat(CustomerIdentityPolicy.validName("x".repeat(101))).isFalse();
  }
}

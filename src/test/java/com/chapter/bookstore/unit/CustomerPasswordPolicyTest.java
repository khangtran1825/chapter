package com.chapter.bookstore.unit;

import static org.assertj.core.api.Assertions.*;

import com.chapter.bookstore.customer.CustomerPasswordPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

class CustomerPasswordPolicyTest {
  @Test
  void countsCodePointsRatherThanBytesOrUtf16() {
    assertThat(CustomerPasswordPolicy.valid("x".repeat(11))).isFalse();
    assertThat(CustomerPasswordPolicy.valid("x".repeat(12))).isTrue();
    assertThat(CustomerPasswordPolicy.valid("😀".repeat(64))).isTrue();
    assertThat(CustomerPasswordPolicy.valid("😀".repeat(65))).isFalse();
    assertThat(CustomerPasswordPolicy.valid(null)).isFalse();
  }

  @Test
  void rawUnicodeAndWhitespaceSurviveEncoding() {
    var encoder = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    String raw = "  Mật khẩu 😀 vẫn giữ nguyên  ";
    String hash = encoder.encode(raw);
    assertThat(encoder.matches(raw, hash)).isTrue();
    assertThat(encoder.matches(raw.strip(), hash)).isFalse();
    assertThat(encoder.matches("😀".repeat(64), encoder.encode("😀".repeat(64)))).isTrue();
  }
}

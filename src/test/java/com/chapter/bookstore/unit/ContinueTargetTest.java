package com.chapter.bookstore.unit;

import static org.assertj.core.api.Assertions.*;

import com.chapter.bookstore.customer.ContinueTarget;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

class ContinueTargetTest {
  @Test
  void permitsOnlyKnownTokens() {
    for (String bad :
        new String[] {
          "https://evil.example",
          "//evil",
          "%2f%2fevil",
          "/admin/books",
          "account/../admin",
          "",
          "CHECKOUT"
        }) assertThat(ContinueTarget.parse(bad)).isEqualTo(ContinueTarget.BOOKS);
    assertThat(ContinueTarget.parse("checkout").path).isEqualTo("/checkout");
  }

  @Test
  void preservesApprovedTokenAcrossRegistrationAndConsumesOnce() {
    var s = new MockHttpSession();
    ContinueTarget.remember(s, "checkout");
    ContinueTarget.remember(s, null);
    assertThat(ContinueTarget.consume(s)).isEqualTo("/checkout");
    assertThat(ContinueTarget.consume(s)).isEqualTo("/books");
  }
}

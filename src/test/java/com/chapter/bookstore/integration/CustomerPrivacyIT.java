package com.chapter.bookstore.integration;

import static org.assertj.core.api.Assertions.*;

import com.chapter.bookstore.shared.PrivatePageFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;

class CustomerPrivacyIT {
  @Test
  void privateRoutesAreNeverCached() throws Exception {
    for (String path :
        new String[] {
          "/login",
          "/register",
          "/account",
          "/logout",
          "/cart",
          "/checkout",
          "/orders/private/confirmation",
          "/admin/books"
        }) {
      var req = new MockHttpServletRequest("GET", path);
      var res = new MockHttpServletResponse();
      new PrivatePageFilter().doFilter(req, res, (a, b) -> {});
      assertThat(res.getHeader("Cache-Control")).isEqualTo("no-store");
    }
  }
}

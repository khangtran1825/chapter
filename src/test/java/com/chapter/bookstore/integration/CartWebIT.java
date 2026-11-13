package com.chapter.bookstore.integration;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class CartWebIT extends PostgresTestSupport {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void setup() {
    Fixtures.reset(jdbc);
  }

  @Test
  void emptyCartIsUsableAndSessionIndependent() throws Exception {
    mvc.perform(get("/cart"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Giỏ sách đang trống")));
    MockHttpSession session = new MockHttpSession();
    mvc.perform(
            post("/cart/items")
                .session(session)
                .with(csrf())
                .param("bookId", Fixtures.BOOK.toString())
                .param("quantity", "2"))
        .andExpect(status().isSeeOther());
    mvc.perform(get("/cart").session(session))
        .andExpect(content().string(containsString("250.000")));
    mvc.perform(get("/cart")).andExpect(content().string(containsString("Giỏ sách đang trống")));
  }

  @Test
  void invalidMutationsAreRejectedWithoutChangingCart() throws Exception {
    MockHttpSession s = new MockHttpSession();
    mvc.perform(
            post("/cart/items")
                .session(s)
                .param("bookId", Fixtures.BOOK.toString())
                .param("quantity", "1"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/cart/items")
                .session(s)
                .with(csrf())
                .param("bookId", Fixtures.BOOK.toString())
                .param("quantity", "4"))
        .andExpect(status().isUnprocessableEntity());
    mvc.perform(
            post("/cart/items")
                .session(s)
                .with(csrf())
                .param("bookId", Fixtures.BOOK.toString())
                .param("quantity", "0"))
        .andExpect(status().isUnprocessableEntity());
  }
}

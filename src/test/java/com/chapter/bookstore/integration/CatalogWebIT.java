package com.chapter.bookstore.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class CatalogWebIT extends PostgresTestSupport {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void seed() {
    Fixtures.reset(jdbc);
  }

  @Test
  void catalogIsPublic() throws Exception {
    mvc.perform(get("/books")).andExpect(status().isOk());
  }

  @Test
  void malformedIdIsBadRequest() throws Exception {
    mvc.perform(get("/books/not-an-id")).andExpect(status().isBadRequest());
  }

  @Test
  void invalidPagesAndLongQueriesAreRejected() throws Exception {
    mvc.perform(get("/books").param("page", "-1")).andExpect(status().isBadRequest());
    mvc.perform(get("/books").param("q", "a".repeat(201))).andExpect(status().isBadRequest());
    mvc.perform(get("/books").param("category", "bad")).andExpect(status().isBadRequest());
  }

  @Test
  void safeDetailsEmptyAndMissingStates() throws Exception {
    jdbc.update("update book set title='<script>alert(1)</script>' where id=?", Fixtures.BOOK);
    mvc.perform(get("/books/" + Fixtures.BOOK))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;script&gt;")));
    mvc.perform(get("/books/" + UUID.randomUUID())).andExpect(status().isNotFound());
    mvc.perform(get("/books").param("q", "nonexistent"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tìm thấy")));
    mvc.perform(get("/")).andExpect(redirectedUrl("/books"));
  }
}

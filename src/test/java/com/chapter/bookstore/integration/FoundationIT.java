package com.chapter.bookstore.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class FoundationIT extends PostgresTestSupport {
  @Autowired MockMvc mvc;

  @Test
  void publicAssetsAndLoginRemainAccessible() throws Exception {
    mvc.perform(get("/css/site.css")).andExpect(status().isOk());
    mvc.perform(get("/admin/login"))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"));
  }

  @Test
  void anonymousAdminRequiresLoginAndUnsafeMethodsRequireCsrf() throws Exception {
    mvc.perform(get("/admin/books")).andExpect(status().is3xxRedirection());
    mvc.perform(post("/admin/books")).andExpect(status().isForbidden());
  }
}

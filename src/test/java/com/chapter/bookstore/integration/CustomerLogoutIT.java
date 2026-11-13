package com.chapter.bookstore.integration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class CustomerLogoutIT extends PostgresTestSupport {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  @Autowired
  @Qualifier("customerPasswordEncoder")
  PasswordEncoder encoder;

  @BeforeEach
  void seed() {
    CustomerFixtures.reset(jdbc);
    CustomerFixtures.create(jdbc, encoder, "logout@example.com", true);
  }

  MockHttpSession login() throws Exception {
    return (MockHttpSession)
        mvc.perform(
                post("/login")
                    .with(csrf())
                    .param("email", "logout@example.com")
                    .param("password", CustomerFixtures.PASSWORD))
            .andExpect(status().isSeeOther())
            .andReturn()
            .getRequest()
            .getSession(false);
  }

  @Test
  void getConfirmsOnlyAndCsrfRequired() throws Exception {
    var s = login();
    mvc.perform(get("/logout").session(s))
        .andExpect(status().isOk())
        .andExpect(view().name("customer/logout"));
    assertThat(s.isInvalid()).isFalse();
    mvc.perform(post("/logout").session(s)).andExpect(status().isForbidden());
    assertThat(s.isInvalid()).isFalse();
  }

  @Test
  void twoIndependentSessionsLogoutOnlyCurrent() throws Exception {
    var a = login();
    var b = login();
    assertThat(a.getId()).isNotEqualTo(b.getId());
    a.setAttribute("cart", "A");
    b.setAttribute("cart", "B");
    a.setAttribute("deliveryDraft", "A draft");
    b.setAttribute("deliveryDraft", "B draft");
    mvc.perform(post("/logout").session(a).with(csrf()))
        .andExpect(status().isSeeOther())
        .andExpect(header().string("Location", "/books?loggedOut"))
        .andExpect(cookie().maxAge("JSESSIONID", 0));
    assertThat(a.isInvalid()).isTrue();
    mvc.perform(get("/account").session(b)).andExpect(status().isOk());
    assertThat(b.getAttribute("cart")).isEqualTo("B");
    assertThat(b.getAttribute("deliveryDraft")).isEqualTo("B draft");
  }

  @Test
  void wrongRealmAndAnonymousLogout() throws Exception {
    mvc.perform(post("/logout").with(user("admin").roles("ADMIN")).with(csrf()))
        .andExpect(status().isForbidden());
    mvc.perform(post("/logout").with(csrf())).andExpect(status().isSeeOther());
  }
}

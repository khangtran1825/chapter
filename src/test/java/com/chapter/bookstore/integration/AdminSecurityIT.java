package com.chapter.bookstore.integration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class AdminSecurityIT extends PostgresTestSupport {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired PasswordEncoder encoder;

  @BeforeEach
  void seed() {
    Fixtures.reset(jdbc);
  }

  @Test
  void managementPagesExistForAuthorizedStaff() throws Exception {
    for (String p :
        new String[] {"/admin/books", "/admin/books/new", "/admin/categories", "/admin/orders"})
      mvc.perform(get(p).with(user("staff").roles("ADMIN"))).andExpect(status().isOk());
  }

  @Test
  void anonymousAndWrongRolesCannotReadOrWrite() throws Exception {
    mvc.perform(get("/admin/orders")).andExpect(status().is3xxRedirection());
    mvc.perform(get("/admin/books").with(user("visitor").roles("CUSTOMER")))
        .andExpect(status().isForbidden());
    mvc.perform(post("/admin/books").with(user("staff").roles("ADMIN")))
        .andExpect(status().isForbidden());
  }

  @Test
  void realLoginRotatesSessionPreservesCartAndLogoutRevokesAccess() throws Exception {
    String name = "staff-" + UUID.randomUUID().toString().substring(0, 8);
    jdbc.update(
        "insert into admin_user values (?,?,?,true,'ADMIN')",
        UUID.randomUUID(),
        name,
        encoder.encode("test-password-only-123"));
    var s = new MockHttpSession();
    s.setAttribute("cart-marker", "retained");
    String old = s.getId();
    var result =
        mvc.perform(
                post("/admin/login")
                    .session(s)
                    .with(r -> {
                      r.setRequestedSessionId(s.getId());
                      r.setRequestedSessionIdValid(true);
                      return r;
                    })
                    .with(csrf())
                    .param("username", name)
                    .param("password", "test-password-only-123"))
            .andExpect(redirectedUrl("/admin/books"))
            .andReturn();
    var logged = (MockHttpSession) result.getRequest().getSession(false);
    assertThat(logged.getId()).isNotEqualTo(old);
    assertThat(logged.getAttribute("cart-marker")).isEqualTo("retained");
    mvc.perform(get("/admin/orders").session(logged)).andExpect(status().isOk());
    mvc.perform(post("/admin/logout").session(logged).with(csrf()))
        .andExpect(redirectedUrl("/admin/login?logout"));
    assertThat(logged.isInvalid()).isTrue();
  }

  @Test
  void everyAdministrativeInterfaceIsProtected() throws Exception {
    String id = Fixtures.BOOK.toString();
    for (String path :
        new String[] {
          "/admin/books",
          "/admin/books/new",
          "/admin/books/" + id + "/edit",
          "/admin/categories",
          "/admin/orders",
          "/admin/orders/" + id
        }) {
      mvc.perform(get(path)).andExpect(status().is3xxRedirection());
      mvc.perform(get(path).with(user("visitor").roles("CUSTOMER")))
          .andExpect(status().isForbidden());
    }
    for (String path :
        new String[] {
          "/admin/books",
          "/admin/books/" + id,
          "/admin/books/" + id + "/inventory",
          "/admin/books/" + id + "/archive",
          "/admin/books/" + id + "/activate",
          "/admin/categories",
          "/admin/categories/" + id,
          "/admin/categories/" + id + "/delete",
          "/admin/orders/" + id + "/status"
        }) {
      mvc.perform(post(path).with(csrf())).andExpect(status().is3xxRedirection());
      mvc.perform(post(path).with(csrf()).with(user("visitor").roles("CUSTOMER")))
          .andExpect(status().isForbidden());
      mvc.perform(post(path).with(user("staff").roles("ADMIN"))).andExpect(status().isForbidden());
    }
  }

  @Test
  void failureThrottlingAndDisabledUsersAreEnforced() throws Exception {
    String name = "blocked-" + UUID.randomUUID().toString().substring(0, 8);
    jdbc.update(
        "insert into admin_user values (?,?,?,false,'ADMIN')",
        UUID.randomUUID(),
        name,
        encoder.encode("test-password-only-123"));
    for (int i = 0; i < 5; i++)
      mvc.perform(
              post("/admin/login")
                  .with(csrf())
                  .param("username", name)
                  .param("password", "test-password-only-123"))
          .andExpect(redirectedUrl("/admin/login?error"));
    mvc.perform(
            post("/admin/login").with(csrf()).param("username", name).param("password", "anything"))
        .andExpect(status().isTooManyRequests());
  }
}

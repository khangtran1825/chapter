package com.chapter.bookstore.integration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.chapter.bookstore.customer.security.CustomerPrincipal;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class CustomerLoginIT extends PostgresTestSupport {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  @Autowired
  @Qualifier("customerPasswordEncoder")
  PasswordEncoder encoder;

  @BeforeEach
  void seed() {
    CustomerFixtures.reset(jdbc);
    CustomerFixtures.create(jdbc, encoder, "login@example.com", true);
  }

  @Test
  void customerCredentialsAuthenticateOnCustomerEndpoint() throws Exception {
    mvc.perform(
            post("/login")
                .with(csrf())
                .param("email", "login@example.com")
                .param("password", CustomerFixtures.PASSWORD))
        .andExpect(status().isSeeOther())
        .andExpect(header().string("Location", "/books"));
  }

  MockHttpSession login() throws Exception {
    return (MockHttpSession)
        mvc.perform(
                post("/login")
                    .with(csrf())
                    .param("email", " LOGIN@example.com ")
                    .param("password", CustomerFixtures.PASSWORD))
            .andExpect(status().isSeeOther())
            .andReturn()
            .getRequest()
            .getSession(false);
  }

  @Test
  void accountOwnDataCredentialsErasedAndRealmProtected() throws Exception {
    var s = login();
    var context = (SecurityContext) s.getAttribute("SPRING_SECURITY_CONTEXT");
    var principal = (CustomerPrincipal) context.getAuthentication().getPrincipal();
    assertThat(principal.getPassword()).isNull();
    assertThat(context.getAuthentication().getCredentials()).isNull();
    mvc.perform(get("/account").session(s))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("login@example.com")))
        .andExpect(
            content()
                .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("pbkdf2"))));
    mvc.perform(get("/admin/books").session(s)).andExpect(status().isForbidden());
    mvc.perform(get("/account").with(user("admin").roles("ADMIN")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/account")).andExpect(redirectedUrl("/login?next=account"));
  }

  @Test
  void wrongUnknownDisabledSameFeedbackAndNoSecret() throws Exception {
    String message = null;
    for (String email :
        new String[] {"login@example.com", "missing@example.com", "disabled@example.com"}) {
      if (email.startsWith("disabled")) CustomerFixtures.create(jdbc, encoder, email, false);
      var result =
          mvc.perform(
                  post("/login")
                      .with(csrf())
                      .param("email", email)
                      .param("password", "wrong-password-123"))
              .andExpect(status().isSeeOther())
              .andExpect(header().string("Location", "/login?error"))
              .andReturn();
      var s = (MockHttpSession) result.getRequest().getSession(false);
      var feedback =
          (com.chapter.bookstore.customer.security.CustomerLoginFeedback)
              s.getAttribute("customerLoginFeedback");
      if (message == null) message = feedback.message();
      assertThat(feedback.message()).isEqualTo(message);
      assertThat(java.util.Collections.list(s.getAttributeNames()))
          .doesNotContain("SPRING_SECURITY_LAST_EXCEPTION");
      mvc.perform(get("/login").session(s))
          .andExpect(status().isOk())
          .andExpect(
              content()
                  .string(
                      org.hamcrest.Matchers.not(
                          org.hamcrest.Matchers.containsString("wrong-password-123"))));
    }
  }

  @Test
  void invalidAndCsrfAndThrottle() throws Exception {
    mvc.perform(post("/login").with(csrf()).param("email", "not-email").param("password", "x"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(forwardedUrl("/login"));
    mvc.perform(
            post("/login")
                .param("email", "login@example.com")
                .param("password", CustomerFixtures.PASSWORD))
        .andExpect(status().isForbidden());
    for (int i = 0; i < 5; i++)
      mvc.perform(
              post("/login")
                  .with(csrf())
                  .param("email", "blocked@example.com")
                  .param("password", "wrong-password-123"))
          .andExpect(status().isSeeOther());
    mvc.perform(
            post("/login")
                .with(csrf())
                .param("email", " BLOCKED@example.com ")
                .param("password", "wrong-password-123"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"));
  }

  @Test
  void customerCannotUseAdminEndpointOrSwitchIdentity() throws Exception {
    mvc.perform(
            post("/admin/login")
                .with(csrf())
                .param("username", "login@example.com")
                .param("password", CustomerFixtures.PASSWORD))
        .andExpect(redirectedUrl("/admin/login?error"));
    var s = login();
    for (String path : new String[] {"/login", "/register", "/admin/login"}) {
      mvc.perform(get(path).session(s)).andExpect(redirectedUrl("/account"));
      mvc.perform(
              post(path)
                  .session(s)
                  .with(csrf())
                  .param("email", "other@example.com")
                  .param("password", CustomerFixtures.PASSWORD))
          .andExpect(status().isSeeOther())
          .andExpect(header().string("Location", "/account"));
    }
  }

  @Autowired PasswordEncoder adminEncoder;

  @Test
  void sameEmailInTwoSourcesNeverFallsBack() throws Exception {
    String email = "dual-" + java.util.UUID.randomUUID() + "@example.com";
    String adminPassword = "fictional-admin-only-123";
    CustomerFixtures.create(jdbc, encoder, email, true);
    String hash = adminEncoder.encode(adminPassword);
    jdbc.update(
        "insert into admin_user values (?,?,?,true,'ADMIN')",
        java.util.UUID.randomUUID(),
        email,
        hash);
    mvc.perform(post("/login").with(csrf()).param("email", email).param("password", adminPassword))
        .andExpect(header().string("Location", "/login?error"));
    mvc.perform(
            post("/admin/login")
                .with(csrf())
                .param("username", email)
                .param("password", CustomerFixtures.PASSWORD))
        .andExpect(redirectedUrl("/admin/login?error"));
    var result =
        mvc.perform(
                post("/admin/login")
                    .with(csrf())
                    .param("username", email)
                    .param("password", adminPassword))
            .andExpect(redirectedUrl("/admin/books"))
            .andReturn();
    var s = (MockHttpSession) result.getRequest().getSession(false);
    mvc.perform(get("/account").session(s)).andExpect(status().isForbidden());
    for (String path : new String[] {"/login", "/register", "/admin/login"}) {
      mvc.perform(get(path).session(s)).andExpect(redirectedUrl("/admin/books"));
      mvc.perform(post(path).session(s).with(csrf()))
          .andExpect(header().string("Location", "/admin/books"));
    }
    assertThat(
            jdbc.queryForObject(
                "select password_hash from admin_user where username=?", String.class, email))
        .isEqualTo(hash);
  }
}

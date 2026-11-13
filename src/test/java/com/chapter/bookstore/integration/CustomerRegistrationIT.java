package com.chapter.bookstore.integration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.chapter.bookstore.cart.CartService;
import com.chapter.bookstore.customer.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class CustomerRegistrationIT extends PostgresTestSupport {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired CustomerRegistrationService registration;
  @Autowired CustomerAccountRepository accounts;

  @Autowired
  @Qualifier("customerPasswordEncoder")
  PasswordEncoder encoder;

  @BeforeEach
  void reset() {
    CustomerFixtures.reset(jdbc);
  }

  @Test
  void registrationPageIsAvailable() throws Exception {
    mvc.perform(get("/register")).andExpect(status().isOk());
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder form(
      String email, String password) {
    return post("/register")
        .with(csrf())
        .param("fullName", "  Bạn đọc  ")
        .param("email", email)
        .param("password", password)
        .param("confirmPassword", password);
  }

  @Test
  void commitsOnlyCustomerPreservesCartAndDoesNotAuthenticate() throws Exception {
    var s = new MockHttpSession();
    CartService.cart(s).add(UUID.randomUUID(), 1, 5);
    mvc.perform(
            form("  Reader+tag@EXAMPLE.COM  ", CustomerFixtures.PASSWORD)
                .session(s)
                .param("role", "ADMIN")
                .param("enabled", "false")
                .param("id", UUID.randomUUID().toString()))
        .andExpect(status().isSeeOther())
        .andExpect(header().string("Location", "/login?registered"));
    var account = accounts.findByEmailNormalized("reader+tag@example.com").orElseThrow();
    assertThat(account.role).isEqualTo("CUSTOMER");
    assertThat(account.enabled).isTrue();
    assertThat(account.fullName).isEqualTo("Bạn đọc");
    assertThat(encoder.matches(CustomerFixtures.PASSWORD, account.passwordHash)).isTrue();
    assertThat(s.getAttribute("SPRING_SECURITY_CONTEXT")).isNull();
    assertThat(CartService.cart(s).snapshot()).hasSize(1);
  }

  @Test
  void errorResponseAndBindingResultContainNoSecrets() throws Exception {
    String secret = "fictional-invalid-password-123";
    var result =
        mvc.perform(form("bad", secret).param("confirmPassword", "different"))
            .andExpect(status().isUnprocessableEntity())
            .andReturn();
    assertThat(result.getResponse().getContentAsString()).doesNotContain(secret, "different");
    var safe = (RegistrationForm) result.getModelAndView().getModel().get("registration");
    assertThat(safe.password).isEmpty();
    assertThat(safe.confirmPassword).isEmpty();
    assertThat(accounts.count()).isZero();
  }

  @Test
  void registrationBindingDoesNotTrimPasswordsAndSupports64Emoji() throws Exception {
    String raw = "  fictional raw secret  ";
    mvc.perform(form("spaces@example.com", raw)).andExpect(status().isSeeOther());
    var a = accounts.findByEmailNormalized("spaces@example.com").orElseThrow();
    assertThat(encoder.matches(raw, a.passwordHash)).isTrue();
    assertThat(encoder.matches(raw.strip(), a.passwordHash)).isFalse();
    mvc.perform(form("emoji@example.com", "😀".repeat(64))).andExpect(status().isSeeOther());
  }

  @Test
  @org.junit.jupiter.api.extension.ExtendWith(
      org.springframework.boot.test.system.OutputCaptureExtension.class)
  void duplicateCaseFoldAndMissingCsrfDoNotMutateExistingAccounts(
      org.springframework.boot.test.system.CapturedOutput output) throws Exception {
    mvc.perform(form("reader@example.com", CustomerFixtures.PASSWORD))
        .andExpect(status().isSeeOther());
    mvc.perform(form("  READER@example.com  ", CustomerFixtures.PASSWORD))
        .andExpect(status().isUnprocessableEntity());
    mvc.perform(post("/register").param("email", "other@example.com"))
        .andExpect(status().isForbidden());
    assertThat(accounts.count()).isEqualTo(1);
    assertThat(output.getAll()).doesNotContain("reader@example.com", CustomerFixtures.PASSWORD);
  }

  @Test
  void concurrentRegistrationsCommitExactlyOnce() throws Exception {
    var gate = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      List<Future<Boolean>> results = new ArrayList<>();
      for (int i = 0; i < 2; i++)
        results.add(
            pool.submit(
                () -> {
                  gate.await();
                  var f = new RegistrationForm();
                  f.fullName = "Người đọc mẫu";
                  f.email = "race@example.com";
                  f.password = CustomerFixtures.PASSWORD;
                  f.confirmPassword = f.password;
                  try {
                    registration.register(f);
                    return true;
                  } catch (CustomerRegistrationService.Rejected expected) {
                    return false;
                  }
                }));
      gate.countDown();
      int successes = 0;
      for (var result : results) if (result.get(15, TimeUnit.SECONDS)) successes++;
      assertThat(successes).isEqualTo(1);
      assertThat(accounts.count()).isEqualTo(1);
    }
  }

  @Test
  void databaseFailureNeverReportsSuccess() throws Exception {
    jdbc.execute(
        "create function auth_test_fail() returns trigger language plpgsql as $$ begin raise"
            + " exception 'fictional database failure'; end $$");
    jdbc.execute(
        "create trigger auth_test_fail before insert on customer_account for each row execute"
            + " function auth_test_fail()");
    try {
      mvc.perform(form("failure@example.com", CustomerFixtures.PASSWORD))
          .andExpect(status().isServiceUnavailable())
          .andExpect(
              content()
                  .string(
                      org.hamcrest.Matchers.not(
                          org.hamcrest.Matchers.containsString(CustomerFixtures.PASSWORD))));
      assertThat(accounts.count()).isZero();
    } finally {
      jdbc.execute("drop trigger auth_test_fail on customer_account");
      jdbc.execute("drop function auth_test_fail()");
    }
  }
}

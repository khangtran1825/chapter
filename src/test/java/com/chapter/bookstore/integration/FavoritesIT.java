package com.chapter.bookstore.integration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class FavoritesIT extends PostgresTestSupport {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  @Autowired
  @Qualifier("customerPasswordEncoder")
  PasswordEncoder encoder;

  UUID owner;
  MockHttpSession session;

  @BeforeEach
  void prepare() throws Exception {
    Fixtures.reset(jdbc);
    CustomerFixtures.reset(jdbc);
    owner = CustomerFixtures.create(jdbc, encoder, "favorite@example.com", true);
    session = login("favorite@example.com");
  }

  MockHttpSession login(String email) throws Exception {
    return (MockHttpSession)
        mvc.perform(
                post("/login")
                    .with(csrf())
                    .param("email", email)
                    .param("password", CustomerFixtures.PASSWORD))
            .andReturn()
            .getRequest()
            .getSession(false);
  }

  @Test
  void addIsIdempotentAndDoesNotChangeShoppingState() throws Exception {
    session.setAttribute("cart", "cart-marker");
    session.setAttribute("deliveryDraft", "draft-marker");
    for (int i = 0; i < 2; i++)
      mvc.perform(post("/books/" + Fixtures.BOOK + "/favorite").session(session).with(csrf()))
          .andExpect(status().isSeeOther())
          .andExpect(header().string("Location", "/books/" + Fixtures.BOOK + "?favoriteSaved"));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from customer_favorite where customer_id=?", Long.class, owner))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject("select stock from book where id=?", Integer.class, Fixtures.BOOK))
        .isEqualTo(3);
    assertThat(jdbc.queryForObject("select count(*) from customer_order", Long.class)).isZero();
    assertThat(session.getAttribute("cart")).isEqualTo("cart-marker");
    assertThat(session.getAttribute("deliveryDraft")).isEqualTo("draft-marker");
  }

  @Test
  void unavailableBookRejectedAndSoldOutAllowed() throws Exception {
    mvc.perform(post("/books/" + UUID.randomUUID() + "/favorite").session(session).with(csrf()))
        .andExpect(status().isNotFound());
    jdbc.update("update book set active=false where id=?", Fixtures.BOOK);
    mvc.perform(post("/books/" + Fixtures.BOOK + "/favorite").session(session).with(csrf()))
        .andExpect(status().isNotFound());
    jdbc.update("update book set active=true,stock=0 where id=?", Fixtures.BOOK);
    mvc.perform(post("/books/" + Fixtures.BOOK + "/favorite").session(session).with(csrf()))
        .andExpect(status().isSeeOther());
  }

  @Test
  void concurrentSavesKeepOneOriginalTimestamp() throws Exception {
    var gate = new java.util.concurrent.CountDownLatch(1);
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(4)) {
      var results = new ArrayList<java.util.concurrent.Future<Integer>>();
      for (int i = 0; i < 4; i++)
        results.add(
            pool.submit(
                () -> {
                  gate.await();
                  return mvc.perform(
                          post("/books/" + Fixtures.BOOK + "/favorite")
                              .session(session)
                              .with(csrf()))
                      .andReturn()
                      .getResponse()
                      .getStatus();
                }));
      gate.countDown();
      for (var f : results)
        assertThat(f.get(15, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(303);
    }
    assertThat(jdbc.queryForObject("select count(*) from customer_favorite", Long.class))
        .isEqualTo(1);
    var before =
        jdbc.queryForObject(
            "select created_at from customer_favorite", java.time.OffsetDateTime.class);
    mvc.perform(post("/books/" + Fixtures.BOOK + "/favorite").session(session).with(csrf()));
    assertThat(
            jdbc.queryForObject(
                "select created_at from customer_favorite", java.time.OffsetDateTime.class))
        .isEqualTo(before);
  }

  @Test
  void listPagesNewestFirstAndRemoveIsIdempotent() throws Exception {
    for (int i = 0; i < 13; i++) {
      UUID id =
          Fixtures.book(
              jdbc, UUID.randomUUID(), "Saved book " + i, "Author", Fixtures.CAT, 3, true);
      jdbc.update(
          "insert into customer_favorite values(?,?,?::timestamptz)",
          owner,
          id,
          "2026-01-01T00:00:" + String.format("%02d", i) + "Z");
    }
    var result =
        mvc.perform(get("/favorites").session(session))
            .andExpect(status().isOk())
            .andExpect(view().name("favorites/list"))
            .andReturn();
    var books =
        (org.springframework.data.domain.Page<?>) result.getModelAndView().getModel().get("books");
    assertThat(books.getTotalElements()).isEqualTo(13);
    assertThat(books.getContent()).hasSize(12);
    mvc.perform(get("/favorites").session(session).param("page", "1"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Saved book 0")));
    UUID id =
        jdbc.queryForObject(
            "select book_id from customer_favorite order by created_at desc limit 1", UUID.class);
    for (int i = 0; i < 2; i++)
      mvc.perform(
              post("/favorites/" + id + "/remove")
                  .session(session)
                  .with(csrf())
                  .param("returnUrl", "https://evil.example"))
          .andExpect(status().isSeeOther())
          .andExpect(header().string("Location", "/favorites"));
    assertThat(jdbc.queryForObject("select count(*) from customer_favorite", Long.class))
        .isEqualTo(12);
  }

  @Test
  void hiddenBooksDisappearAndRestoreWithoutLosingFavorite() throws Exception {
    jdbc.update(
        "insert into customer_favorite(customer_id,book_id) values(?,?)", owner, Fixtures.BOOK);
    jdbc.update("update book set active=false where id=?", Fixtures.BOOK);
    mvc.perform(get("/favorites").session(session))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có sách yêu thích")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Những ngày xanh"))));
    jdbc.update("update book set active=true,stock=0 where id=?", Fixtures.BOOK);
    mvc.perform(get("/favorites").session(session))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Những ngày xanh")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Tạm hết sách")));
  }

  @Test
  void permissionCsrfAndInputValidation() throws Exception {
    mvc.perform(get("/favorites")).andExpect(redirectedUrl("/login?next=favorites"));
    mvc.perform(post("/books/" + Fixtures.BOOK + "/favorite").with(csrf()))
        .andExpect(redirectedUrl("/login?next=favorites"));
    for (String path :
        new String[] {
          "/books/" + Fixtures.BOOK + "/favorite", "/favorites/" + Fixtures.BOOK + "/remove"
        }) {
      mvc.perform(post(path).session(session)).andExpect(status().isForbidden());
      mvc.perform(post(path).with(user("staff").roles("ADMIN")).with(csrf()))
          .andExpect(status().isForbidden());
    }
    mvc.perform(get("/favorites").with(user("staff").roles("ADMIN")))
        .andExpect(status().isForbidden());
    mvc.perform(post("/books/not-a-uuid/favorite").session(session).with(csrf()))
        .andExpect(status().isBadRequest());
    for (String page : new String[] {"-1", "100001", "abc"})
      mvc.perform(get("/favorites").session(session).param("page", page))
          .andExpect(status().isBadRequest());
    mvc.perform(get("/favorites").session(session))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    assertThat(jdbc.queryForObject("select count(*) from customer_favorite", Long.class)).isZero();
  }

  @Test
  void ownersComeOnlyFromPrincipalAndPersistAfterLogout() throws Exception {
    UUID other = CustomerFixtures.create(jdbc, encoder, "other-favorite@example.com", true);
    mvc.perform(
            post("/books/" + Fixtures.BOOK + "/favorite")
                .session(session)
                .with(csrf())
                .param("customerId", other.toString()))
        .andExpect(status().isSeeOther());
    var b = login("other-favorite@example.com");
    mvc.perform(get("/favorites").session(b).param("customerId", owner.toString()))
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("Chưa có sách yêu thích")));
    mvc.perform(
            post("/favorites/" + Fixtures.BOOK + "/remove")
                .session(b)
                .with(csrf())
                .param("customerId", owner.toString()))
        .andExpect(status().isSeeOther());
    assertThat(jdbc.queryForObject("select customer_id from customer_favorite", UUID.class))
        .isEqualTo(owner);
    var another = login("favorite@example.com");
    mvc.perform(get("/favorites").session(another))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Những ngày xanh")));
    mvc.perform(post("/logout").session(session).with(csrf())).andExpect(status().isSeeOther());
    mvc.perform(get("/favorites").session(another)).andExpect(status().isOk());
    var fresh = login("favorite@example.com");
    mvc.perform(get("/favorites").session(fresh))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Những ngày xanh")));
  }

  @Test
  void favoritesLoginTargetDoesNotReplayPost() throws Exception {
    var guest = new MockHttpSession();
    mvc.perform(get("/login").session(guest).param("next", "favorites")).andExpect(status().isOk());
    mvc.perform(
            post("/login")
                .session(guest)
                .with(csrf())
                .param("email", "favorite@example.com")
                .param("password", CustomerFixtures.PASSWORD))
        .andExpect(header().string("Location", "/favorites"));
    assertThat(jdbc.queryForObject("select count(*) from customer_favorite", Long.class)).isZero();
  }

  @Test
  void databaseFailureIsSafeAndNeverReportsSaved() throws Exception {
    jdbc.execute(
        "create function favorites_test_fail() returns trigger language plpgsql as $$ begin raise"
            + " exception 'fictional failure'; end $$");
    jdbc.execute(
        "create trigger favorites_test_fail before insert on customer_favorite for each row execute"
            + " function favorites_test_fail()");
    try {
      mvc.perform(post("/books/" + Fixtures.BOOK + "/favorite").session(session).with(csrf()))
          .andExpect(status().isServiceUnavailable())
          .andExpect(header().doesNotExist("Location"))
          .andExpect(
              content()
                  .string(
                      org.hamcrest.Matchers.not(
                          org.hamcrest.Matchers.containsString(CustomerFixtures.PASSWORD))));
      assertThat(jdbc.queryForObject("select count(*) from customer_favorite", Long.class))
          .isZero();
    } finally {
      jdbc.execute("drop trigger favorites_test_fail on customer_favorite");
      jdbc.execute("drop function favorites_test_fail()");
    }
  }
}

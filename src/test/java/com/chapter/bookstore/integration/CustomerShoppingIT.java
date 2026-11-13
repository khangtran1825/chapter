package com.chapter.bookstore.integration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.chapter.bookstore.cart.Cart;
import com.chapter.bookstore.checkout.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class CustomerShoppingIT extends PostgresTestSupport {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  @Autowired
  @Qualifier("customerPasswordEncoder")
  PasswordEncoder encoder;

  @BeforeEach
  void seed() {
    CustomerFixtures.reset(jdbc);
    CustomerFixtures.create(jdbc, encoder, "shopping@example.com", true);
  }

  @Test
  void cartsDraftAndReceiptOwnershipSurviveRotationWithoutOrders() throws Exception {
    for (int count : new int[] {1, 2, 100}) {
      var s = new MockHttpSession();
      var cart = new Cart();
      for (int i = 0; i < count; i++) cart.add(UUID.randomUUID(), i + 1, 100);
      var snapshot = cart.snapshot();
      long version = cart.version();
      var checkout = CheckoutSession.get(s);
      var draft = new DeliveryForm();
      s.setAttribute("cart", cart);
      s.setAttribute("deliveryDraft", draft);
      s.setAttribute("unapproved", "discard");
      String old = s.getId();
      long orders = jdbc.queryForObject("select count(*) from customer_order", Long.class);
      mvc.perform(get("/login").session(s).param("next", "cart")).andExpect(status().isOk());
      var result =
          mvc.perform(
                  post("/login")
                      .session(s)
                      .with(
                          r -> {
                            r.setRequestedSessionId(s.getId());
                            r.setRequestedSessionIdValid(true);
                            return r;
                          })
                      .with(csrf())
                      .param("email", "shopping@example.com")
                      .param("password", CustomerFixtures.PASSWORD))
              .andExpect(status().isSeeOther())
              .andExpect(header().string("Location", "/cart"))
              .andReturn();
      var rotated = result.getRequest().getSession(false);
      assertThat(s.isInvalid()).isTrue();
      assertThat(rotated.getId()).isNotEqualTo(old);
      assertThat(rotated.getAttribute("cart")).isSameAs(cart);
      assertThat(cart.snapshot()).isEqualTo(snapshot);
      assertThat(cart.version()).isEqualTo(version);
      assertThat(rotated.getAttribute("checkout")).isSameAs(checkout);
      assertThat(rotated.getAttribute("deliveryDraft")).isSameAs(draft);
      assertThat(rotated.getAttribute("unapproved")).isNull();
      assertThat(jdbc.queryForObject("select count(*) from customer_order", Long.class))
          .isEqualTo(orders);
    }
  }

  @Test
  void targetAllowlistAndEmptyCheckout() throws Exception {
    for (String target :
        new String[] {
          "https://evil.example", "/admin/books", "%2F%2Fevil.example", "account", "checkout"
        }) {
      var s = new MockHttpSession();
      mvc.perform(get("/register").session(s).param("next", target)).andExpect(status().isOk());
      mvc.perform(get("/login").session(s)).andExpect(status().isOk());
      String path =
          target.equals("account")
              ? "/account"
              : target.equals("checkout") ? "/checkout" : "/books";
      var result =
          mvc.perform(
                  post("/login")
                      .session(s)
                      .with(
                          r -> {
                            r.setRequestedSessionId(s.getId());
                            r.setRequestedSessionIdValid(true);
                            return r;
                          })
                      .with(csrf())
                      .param("email", "shopping@example.com")
                      .param("password", CustomerFixtures.PASSWORD))
              .andExpect(header().string("Location", path))
              .andReturn();
      if (path.equals("/checkout"))
        mvc.perform(get(path).session((MockHttpSession) result.getRequest().getSession(false)))
            .andExpect(redirectedUrl("/cart"));
    }
  }
}

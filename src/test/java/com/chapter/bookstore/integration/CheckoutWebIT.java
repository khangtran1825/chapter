package com.chapter.bookstore.integration;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.chapter.bookstore.cart.CartService;
import com.chapter.bookstore.checkout.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class CheckoutWebIT extends PostgresTestSupport {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void setup() {
    Fixtures.reset(jdbc);
  }

  @Test
  void checkoutReviewIsAvailableForPreparedCart() throws Exception {
    var s = new MockHttpSession();
    mvc.perform(
            post("/cart/items")
                .session(s)
                .with(csrf())
                .param("bookId", Fixtures.BOOK.toString())
                .param("quantity", "1"))
        .andExpect(status().isSeeOther());
    mvc.perform(get("/checkout").session(s)).andExpect(status().isOk());
  }

  private MockHttpSession prepared() throws Exception {
    var s = new MockHttpSession();
    mvc.perform(
        post("/cart/items")
            .session(s)
            .with(csrf())
            .param("bookId", Fixtures.BOOK.toString())
            .param("quantity", "1"));
    return s;
  }

  private UUID quote(MockHttpSession s) throws Exception {
    var result = mvc.perform(get("/checkout").session(s)).andReturn();
    return ((CheckoutQuote) result.getModelAndView().getModel().get("quote")).key();
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder submit(
      MockHttpSession s, UUID key) {
    return post("/checkout")
        .session(s)
        .with(csrf())
        .param("purchaseKey", key.toString())
        .param("recipientName", "Tên hợp lệ")
        .param("phone", "0901234567")
        .param("street", "12 Đường Mẫu")
        .param("ward", "Phường Mẫu")
        .param("district", "Quận Mẫu")
        .param("province", "Hà Nội");
  }

  @Test
  void validationPreservesFieldsAndCart() throws Exception {
    var s = prepared();
    var key = quote(s);
    var result =
        mvc.perform(submit(s, key).param("phone", "bad"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(model().attributeHasFieldErrors("delivery", "phone"))
            .andReturn();
    org.assertj.core.api.Assertions.assertThat(
            ((DeliveryForm) result.getModelAndView().getModel().get("delivery")).recipientName)
        .isEqualTo("Tên hợp lệ");
    org.assertj.core.api.Assertions.assertThat(CartService.cart(s).snapshot()).hasSize(1);
  }

  @Test
  void receiptIsPrivateAndLostResponseRetryDoesNotDuplicate() throws Exception {
    var s = prepared();
    var key = quote(s);
    var result = mvc.perform(submit(s, key)).andExpect(status().isSeeOther()).andReturn();
    String location = result.getResponse().getHeader("Location");
    mvc.perform(get(location).session(s))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"));
    mvc.perform(get(location)).andExpect(status().isNotFound());
    CartService.cart(s).add(Fixtures.BOOK, 1, 2);
    mvc.perform(submit(s, key))
        .andExpect(status().isSeeOther())
        .andExpect(header().string("Location", location));
    org.assertj.core.api.Assertions.assertThat(CartService.cart(s).snapshot().get(Fixtures.BOOK))
        .isEqualTo(1);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject("select count(*) from customer_order", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void emptyCartRedirectsAndLaterMutationInvalidatesQuote() throws Exception {
    mvc.perform(get("/checkout")).andExpect(redirectedUrl("/cart"));
    var s = prepared();
    var key = quote(s);
    CartService.cart(s).add(Fixtures.BOOK, 1, 3);
    mvc.perform(submit(s, key)).andExpect(status().isConflict());
    org.assertj.core.api.Assertions.assertThat(CartService.cart(s).snapshot().get(Fixtures.BOOK))
        .isEqualTo(2);
  }
}

package com.chapter.bookstore.integration;

import static org.assertj.core.api.Assertions.*;

import com.chapter.bookstore.admin.*;
import com.chapter.bookstore.catalog.*;
import com.chapter.bookstore.checkout.*;
import com.chapter.bookstore.order.*;
import com.chapter.bookstore.shared.WebProblem;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class AdminManagementIT extends PostgresTestSupport {
  @Autowired AdminCatalogService admin;
  @Autowired OrderManagementService management;
  @Autowired OrderRepository orders;
  @Autowired CheckoutService checkout;
  @Autowired BookRepository books;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void seed() {
    Fixtures.reset(jdbc);
  }

  CustomerOrder purchase() {
    var c = CheckoutIT.cart();
    return checkout.place(checkout.quote(c), "owner", CheckoutIT.delivery(), c);
  }

  @Test
  void preservesPurchaseHistoryAndRejectsStaleEdits() {
    var order = purchase();
    var before = books.findById(Fixtures.BOOK).orElseThrow();
    var form = BookForm.from(before);
    form.title = "Tên mới";
    form.priceVnd = 50000L;
    admin.save(before.id, form);
    assertThatThrownBy(() -> admin.save(before.id, form)).isInstanceOf(WebProblem.class);
    var current = books.findById(before.id).orElseThrow();
    admin.active(before.id, false, current.version);
    var stored = orders.findById(order.id).orElseThrow();
    assertThat(stored.lines.getFirst().titleSnapshot).isEqualTo("Những ngày xanh");
    assertThat(stored.lines.getFirst().unitPriceVnd).isEqualTo(125000);
  }

  @Test
  void validatesBooksCategoriesAndDeletionRestriction() {
    var f = BookForm.from(books.findById(Fixtures.BOOK).orElseThrow());
    f.priceVnd = -1L;
    assertThatThrownBy(() -> admin.save(null, f)).isInstanceOf(WebProblem.class);
    f.priceVnd = 0L;
    f.coverPath = "https://evil.example/image.svg";
    assertThatThrownBy(() -> admin.save(null, f)).isInstanceOf(WebProblem.class);
    assertThatThrownBy(() -> admin.deleteCategory(Fixtures.CAT)).isInstanceOf(WebProblem.class);
    assertThatThrownBy(() -> admin.category(null, " ")).isInstanceOf(WebProblem.class);
  }

  @Test
  void followsStateMachineAndRestoresStockOnce() {
    var o = purchase();
    management.transition(o.id, OrderStatus.PENDING, OrderStatus.CONFIRMED);
    management.transition(o.id, OrderStatus.CONFIRMED, OrderStatus.CANCELLED);
    management.transition(o.id, OrderStatus.CONFIRMED, OrderStatus.CANCELLED);
    assertThat(
            jdbc.queryForObject("select stock from book where id=?", Integer.class, Fixtures.BOOK))
        .isEqualTo(3);
    assertThatThrownBy(
            () -> management.transition(o.id, OrderStatus.CANCELLED, OrderStatus.SHIPPING))
        .isInstanceOf(WebProblem.class);
  }

  @Test
  void completedOrdersCannotCancel() {
    var o = purchase();
    management.transition(o.id, OrderStatus.PENDING, OrderStatus.CONFIRMED);
    management.transition(o.id, OrderStatus.CONFIRMED, OrderStatus.SHIPPING);
    management.transition(o.id, OrderStatus.SHIPPING, OrderStatus.COMPLETED);
    assertThatThrownBy(
            () -> management.transition(o.id, OrderStatus.COMPLETED, OrderStatus.CANCELLED))
        .isInstanceOf(WebProblem.class);
  }

  @Test
  void concurrentCancellationRestoresOnlyOnce() throws Exception {
    var o = purchase();
    var r =
        CheckoutIT.concurrent(
            () -> {
              management.transition(o.id, OrderStatus.PENDING, OrderStatus.CANCELLED);
              return "done";
            },
            () -> {
              management.transition(o.id, OrderStatus.PENDING, OrderStatus.CANCELLED);
              return "done";
            });
    assertThat(r).containsOnly("done");
    assertThat(
            jdbc.queryForObject("select stock from book where id=?", Integer.class, Fixtures.BOOK))
        .isEqualTo(3);
  }

  @Test
  void inventoryEditCannotOverwriteCheckoutDeduction() throws Exception {
    var c = CheckoutIT.cart();
    var q = checkout.quote(c);
    CheckoutIT.concurrent(
        () -> {
          admin.inventory(Fixtures.BOOK, 7, 0);
          return "updated";
        },
        () -> checkout.place(q, "owner", CheckoutIT.delivery(), c).reference);
    int count = jdbc.queryForObject("select count(*) from customer_order", Integer.class);
    assertThat(
            jdbc.queryForObject("select stock from book where id=?", Integer.class, Fixtures.BOOK))
        .isEqualTo(count == 1 ? 2 : 7);
  }
}

package com.chapter.bookstore.integration;

import static org.assertj.core.api.Assertions.*;

import com.chapter.bookstore.cart.Cart;
import com.chapter.bookstore.checkout.*;
import com.chapter.bookstore.shared.WebProblem;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class CheckoutIT extends PostgresTestSupport {
  @Autowired CheckoutService checkout;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void seed() {
    Fixtures.reset(jdbc);
  }

  public static DeliveryForm delivery() {
    var f = new DeliveryForm();
    f.recipientName = "Khách thử nghiệm";
    f.phone = "0901234567";
    f.street = "12 Đường Sách";
    f.ward = "Phường Mẫu";
    f.district = "Quận Mẫu";
    f.province = "Hà Nội";
    return f;
  }

  public static Cart cart() {
    var c = new Cart();
    c.add(Fixtures.BOOK, 1, 3);
    return c;
  }

  @Test
  void createsOneOrderAndRetriesAfterCartCleared() {
    var c = cart();
    var q = checkout.quote(c);
    var o = checkout.place(q, "owner", delivery(), c);
    c.clearIfVersion(q.cartVersion());
    var retry = checkout.place(q, "owner", delivery(), c);
    assertThat(retry.id).isEqualTo(o.id);
    assertThat(
            jdbc.queryForObject("select stock from book where id=?", Integer.class, Fixtures.BOOK))
        .isEqualTo(2);
    assertThat(o.totalVnd).isEqualTo(125000);
    assertThat(o.lines).hasSize(1);
  }

  @Test
  void changedPayloadOrOwnerCannotReuseKey() {
    var c = cart();
    var q = checkout.quote(c);
    checkout.place(q, "owner", delivery(), c);
    var f = delivery();
    f.street = "Địa chỉ khác";
    assertThatThrownBy(() -> checkout.place(q, "owner", f, c)).isInstanceOf(WebProblem.class);
    assertThatThrownBy(() -> checkout.place(q, "different", delivery(), c))
        .isInstanceOf(WebProblem.class);
  }

  @Test
  void priceChangeRollsBackAndSnapshotsDoNotChange() {
    var c = cart();
    var q = checkout.quote(c);
    jdbc.update("update book set price_vnd=99000 where id=?", Fixtures.BOOK);
    assertThatThrownBy(() -> checkout.place(q, "owner", delivery(), c))
        .isInstanceOf(WebProblem.class);
    assertThat(jdbc.queryForObject("select count(*) from customer_order", Integer.class)).isZero();
    assertThat(
            jdbc.queryForObject("select stock from book where id=?", Integer.class, Fixtures.BOOK))
        .isEqualTo(3);
    var o = checkout.place(checkout.quote(c), "owner", delivery(), c);
    jdbc.update("update book set title='Đổi tên',price_vnd=50000 where id=?", Fixtures.BOOK);
    assertThat(checkout.receipt(o.reference, "owner").lines.getFirst().unitPriceVnd)
        .isEqualTo(99000);
  }

  @Test
  void invalidDeliveryAndArchivedBookCannotCreateOrders() {
    var c = cart();
    var q = checkout.quote(c);
    var bad = delivery();
    bad.phone = "123";
    assertThatThrownBy(() -> checkout.place(q, "owner", bad, c)).isInstanceOf(WebProblem.class);
    jdbc.update("update book set active=false where id=?", Fixtures.BOOK);
    assertThatThrownBy(() -> checkout.place(q, "owner", delivery(), c))
        .isInstanceOf(WebProblem.class);
    assertThat(jdbc.queryForObject("select count(*) from customer_order", Integer.class)).isZero();
  }

  @Test
  void twoShoppersCannotBuyTheFinalCopy() throws Exception {
    jdbc.update("update book set stock=1 where id=?", Fixtures.BOOK);
    var a = cart();
    var b = cart();
    var qa = checkout.quote(a);
    var qb = checkout.quote(b);
    var results =
        concurrent(
            () -> checkout.place(qa, "a", delivery(), a).reference,
            () -> checkout.place(qb, "b", delivery(), b).reference);
    assertThat(results.stream().filter(x -> x.startsWith("CH-")).count()).isEqualTo(1);
    assertThat(
            jdbc.queryForObject("select stock from book where id=?", Integer.class, Fixtures.BOOK))
        .isZero();
    assertThat(jdbc.queryForObject("select count(*) from customer_order", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void concurrentSameKeyCreatesOneOrder() throws Exception {
    var c = cart();
    var q = checkout.quote(c);
    var r =
        concurrent(
            () -> checkout.place(q, "same", delivery(), c).reference,
            () -> checkout.place(q, "same", delivery(), c).reference);
    assertThat(r).hasSize(2);
    assertThat(r.get(0)).startsWith("CH-").isEqualTo(r.get(1));
    assertThat(jdbc.queryForObject("select count(*) from customer_order", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void secondLineConflictRollsBackEarlierStockChanges() {
    UUID second = UUID.fromString("30000000-0000-0000-0000-000000000001");
    Fixtures.book(jdbc, second, "Second book", "Sample", Fixtures.CAT, 3, true);
    var c = cart();
    c.add(second, 1, 3);
    var q = checkout.quote(c);
    jdbc.update("update book set price_vnd=99000 where id=?", second);
    assertThatThrownBy(() -> checkout.place(q, "owner", delivery(), c))
        .isInstanceOf(WebProblem.class);
    assertThat(
            jdbc.queryForObject("select stock from book where id=?", Integer.class, Fixtures.BOOK))
        .isEqualTo(3);
    assertThat(jdbc.queryForObject("select count(*) from customer_order", Integer.class)).isZero();
  }

  static List<String> concurrent(Callable<String> a, Callable<String> b) throws Exception {
    var gate = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      List<Future<String>> futures = new ArrayList<>();
      for (var task : List.of(a, b))
        futures.add(
            pool.submit(() -> {
              gate.await();
              try {
                return task.call();
              } catch (WebProblem ex) {
                return "rejected";
              }
            }));
      gate.countDown();
      List<String> result = new ArrayList<>();
      for (var f : futures) result.add(f.get(15, TimeUnit.SECONDS));
      return result;
    }
  }
}

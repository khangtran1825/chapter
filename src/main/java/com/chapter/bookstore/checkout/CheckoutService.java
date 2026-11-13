package com.chapter.bookstore.checkout;

import com.chapter.bookstore.cart.Cart;
import com.chapter.bookstore.catalog.*;
import com.chapter.bookstore.order.*;
import com.chapter.bookstore.shared.*;
import jakarta.validation.Validator;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutService {
  private final BookRepository books;
  private final OrderRepository orders;
  private final CheckoutLockRepository locks;
  private final Validator validator;

  public CheckoutService(
      BookRepository b, OrderRepository o, CheckoutLockRepository l, Validator v) {
    books = b;
    orders = o;
    locks = l;
    validator = v;
  }

  @Transactional(readOnly = true)
  public CheckoutQuote quote(Cart cart) {
    synchronized (cart) {
      Map<UUID, CheckoutQuote.Line> lines = new LinkedHashMap<>();
      for (var e : cart.snapshot().entrySet()) {
        var b =
            books
                .findById(e.getKey())
                .orElseThrow(() -> new WebProblem(409, "Sách không còn được bán."));
        if (!b.active || e.getValue() > b.stock)
          throw new WebProblem(409, "Một số sách không đủ số lượng. Vui lòng cập nhật giỏ.");
        lines.put(b.id, new CheckoutQuote.Line(b.id, b.title, b.priceVnd, e.getValue(), b.stock));
      }
      if (lines.isEmpty()) throw new WebProblem(409, "Giỏ sách đang trống.");
      return new CheckoutQuote(UUID.randomUUID(), cart.version(), lines);
    }
  }

  @Transactional(timeout = 10)
  public CustomerOrder place(CheckoutQuote quote, String owner, DeliveryForm form, Cart cart) {
    if (!validator.validate(form).isEmpty())
      throw new WebProblem(422, "Vui lòng kiểm tra thông tin giao hàng.");
    String fingerprint = fingerprint(quote, form);
    locks.lock(quote.key());
    var existing = orders.findByCheckoutKey(quote.key());
    if (existing.isPresent()) {
      var o = existing.get();
      if (!ReceiptAccess.owns(o.ownerTokenHash, owner) || !o.requestFingerprint.equals(fingerprint))
        throw new WebProblem(409, "Mã thanh toán đã được sử dụng với thông tin khác.");
      return o;
    }
    synchronized (cart) {
      if (cart.version() != quote.cartVersion())
        throw new WebProblem(409, "Giỏ sách đã thay đổi. Vui lòng xem lại trước khi đặt hàng.");
    }
    var order = new CustomerOrder();
    order.reference =
        "CH-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    order.checkoutKey = quote.key();
    order.requestFingerprint = fingerprint;
    order.ownerTokenHash = ReceiptAccess.hash(owner);
    order.recipientName = form.recipientName;
    order.phone = form.phone;
    order.street = form.street;
    order.ward = form.ward;
    order.district = form.district;
    order.province = form.province;
    for (var id : quote.lines().keySet().stream().sorted().toList()) {
      var item = quote.lines().get(id);
      var b = books.locked(id).orElseThrow(() -> new WebProblem(409, "Sách không còn tồn tại."));
      if (!b.active
          || b.stock != item.stock()
          || b.stock < item.quantity()
          || b.priceVnd != item.price())
        throw new WebProblem(
            409, "Giá hoặc tồn kho đã thay đổi. Vui lòng xem lại và xác nhận lại.");
      if (item.quantity() < 1) throw new WebProblem(422, "Số lượng không hợp lệ.");
      OrderLine line = new OrderLine();
      line.order = order;
      line.bookId = id;
      line.titleSnapshot = b.title;
      line.authorSnapshot = b.author;
      line.unitPriceVnd = b.priceVnd;
      line.quantity = item.quantity();
      line.lineTotalVnd = Money.multiply(b.priceVnd, item.quantity());
      order.lines.add(line);
      order.subtotalVnd = Money.add(order.subtotalVnd, line.lineTotalVnd);
      b.stock -= item.quantity();
    }
    order.totalVnd = order.subtotalVnd;
    return orders.saveAndFlush(order);
  }

  private String fingerprint(CheckoutQuote q, DeliveryForm f) {
    StringBuilder s = new StringBuilder();
    for (var id : q.lines().keySet().stream().sorted().toList()) {
      var l = q.lines().get(id);
      s.append(id).append(':').append(l.quantity()).append(':').append(l.price()).append(';');
    }
    for (var part : List.of(f.recipientName, f.phone, f.street, f.ward, f.district, f.province))
      s.append(part.length()).append(':').append(part);
    return ReceiptAccess.hash(s.toString());
  }

  @Transactional(readOnly = true)
  public CustomerOrder receipt(String reference, String owner) {
    var o =
        orders
            .findByReference(reference)
            .orElseThrow(() -> new WebProblem(404, "Không tìm thấy đơn hàng."));
    if (!ReceiptAccess.owns(o.ownerTokenHash, owner))
      throw new WebProblem(404, "Không tìm thấy đơn hàng.");
    return o;
  }
}

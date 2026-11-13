package com.chapter.bookstore.order;

import com.chapter.bookstore.catalog.BookRepository;
import com.chapter.bookstore.shared.WebProblem;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderManagementService {
  private final OrderRepository orders;
  private final BookRepository books;
  private final JdbcTemplate jdbc;

  public OrderManagementService(OrderRepository o, BookRepository b, JdbcTemplate j) {
    orders = o;
    books = b;
    jdbc = j;
  }

  @Transactional(timeout = 10)
  public void transition(UUID id, OrderStatus expected, OrderStatus target) {
    jdbc.execute("SET LOCAL lock_timeout = '3s'");
    var o = orders.locked(id).orElseThrow(() -> new WebProblem(404, "Không tìm thấy đơn hàng."));
    if (o.status == target) return;
    if (o.status != expected || !o.status.allows(target))
      throw new WebProblem(
          409, "Trạng thái đơn hàng vừa thay đổi hoặc chuyển trạng thái không hợp lệ.");
    if (target == OrderStatus.CANCELLED) {
      for (var line : o.lines.stream().sorted(Comparator.comparing(l -> l.bookId)).toList()) {
        var b =
            books
                .locked(line.bookId)
                .orElseThrow(() -> new WebProblem(409, "Không tìm thấy sách để hoàn kho."));
        try {
          b.stock = Math.addExact(b.stock, line.quantity);
        } catch (ArithmeticException ex) {
          throw new WebProblem(409, "Tồn kho vượt giới hạn.");
        }
      }
    }
    o.status = target;
    orders.flush();
  }
}

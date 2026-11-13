package com.chapter.bookstore.admin;

import com.chapter.bookstore.order.*;
import com.chapter.bookstore.shared.WebProblem;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AdminOrderController {
  private final OrderRepository orders;
  private final OrderManagementService service;

  public AdminOrderController(OrderRepository o, OrderManagementService s) {
    orders = o;
    service = s;
  }

  @GetMapping("/admin/orders")
  String list(
      @RequestParam(required = false) OrderStatus status,
      @RequestParam(defaultValue = "0") int page,
      Model m) {
    if (page < 0 || page > 100000) throw new WebProblem(400, "Số trang không hợp lệ.");
    var p = PageRequest.of(page, 20, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")));
    m.addAttribute("orders", status == null ? orders.findAll(p) : orders.findByStatus(status, p));
    m.addAttribute("statuses", OrderStatus.values());
    m.addAttribute("selectedStatus", status);
    return "admin/orders";
  }

  @GetMapping("/admin/orders/{id}")
  String detail(@PathVariable UUID id, Model m) {
    m.addAttribute(
        "order",
        orders.findById(id).orElseThrow(() -> new WebProblem(404, "Không tìm thấy đơn hàng.")));
    m.addAttribute("statuses", OrderStatus.values());
    return "admin/order-detail";
  }

  @PostMapping("/admin/orders/{id}/status")
  String status(
      @PathVariable UUID id,
      @RequestParam OrderStatus expected,
      @RequestParam OrderStatus target,
      HttpServletResponse res) {
    service.transition(id, expected, target);
    res.setStatus(303);
    res.setHeader("Location", "/admin/orders/" + id);
    return null;
  }
}

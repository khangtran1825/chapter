package com.chapter.bookstore.cart;

import com.chapter.bookstore.catalog.CatalogService;
import com.chapter.bookstore.shared.WebProblem;
import jakarta.servlet.http.*;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class CartController {
  private final CartService service;
  private final CatalogService catalog;

  public CartController(CartService service, CatalogService catalog) {
    this.service = service;
    this.catalog = catalog;
  }

  @GetMapping("/cart")
  String view(HttpSession session, Model model) {
    model.addAttribute("cartView", service.view(CartService.cart(session)));
    return "cart/view";
  }

  @PostMapping("/cart/items")
  String add(
      @RequestParam UUID bookId,
      @RequestParam int quantity,
      HttpSession session,
      HttpServletResponse res,
      Model model) {
    try {
      var b = catalog.book(bookId);
      CartService.cart(session).add(bookId, quantity, b.stock);
    } catch (WebProblem ex) {
      return error(ex, session, res, model);
    }
    return redirect(res);
  }

  @PostMapping("/cart/items/{id}/quantity")
  String quantity(
      @PathVariable UUID id,
      @RequestParam int quantity,
      HttpSession s,
      HttpServletResponse res,
      Model model) {
    try {
      var b = catalog.book(id);
      CartService.cart(s).set(id, quantity, b.stock);
    } catch (WebProblem ex) {
      return error(ex, s, res, model);
    }
    return redirect(res);
  }

  @PostMapping("/cart/items/{id}/remove")
  String remove(@PathVariable UUID id, HttpSession s, HttpServletResponse res) {
    CartService.cart(s).remove(id);
    return redirect(res);
  }

  private String redirect(HttpServletResponse res) {
    res.setStatus(303);
    res.setHeader("Location", "/cart");
    return null;
  }

  private String error(WebProblem ex, HttpSession s, HttpServletResponse res, Model model) {
    res.setStatus(ex.status);
    model.addAttribute("message", ex.getMessage());
    return view(s, model);
  }
}

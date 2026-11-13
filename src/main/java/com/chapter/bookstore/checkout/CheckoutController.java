package com.chapter.bookstore.checkout;

import com.chapter.bookstore.cart.*;
import com.chapter.bookstore.shared.WebProblem;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class CheckoutController {
  private final CheckoutService checkout;
  private final CartService carts;

  public CheckoutController(CheckoutService c, CartService carts) {
    checkout = c;
    this.carts = carts;
  }

  @GetMapping("/checkout")
  String review(HttpSession session, Model model) {
    var cart = CartService.cart(session);
    if (cart.snapshot().isEmpty()) return "redirect:/cart";
    var quote = checkout.quote(cart);
    CheckoutSession.get(session).remember(quote);
    model.addAttribute(
        "delivery",
        session.getAttribute("deliveryDraft") instanceof DeliveryForm f ? f : new DeliveryForm());
    return page(quote, session, model);
  }

  @PostMapping("/checkout")
  String submit(
      @RequestParam UUID purchaseKey,
      @Valid @ModelAttribute("delivery") DeliveryForm form,
      BindingResult errors,
      HttpSession s,
      HttpServletResponse res,
      Model model) {
    var state = CheckoutSession.get(s);
    var quote = state.quote(purchaseKey);
    s.setAttribute("deliveryDraft", form);
    if (errors.hasErrors()) {
      res.setStatus(422);
      return page(quote, s, model);
    }
    var cart = CartService.cart(s);
    try {
      var order = checkout.place(quote, state.ownerToken, form, cart);
      cart.clearIfVersion(quote.cartVersion());
      s.removeAttribute("deliveryDraft");
      res.setStatus(303);
      res.setHeader("Location", "/orders/" + order.reference + "/confirmation");
      return null;
    } catch (WebProblem ex) {
      res.setStatus(ex.status);
      model.addAttribute("message", ex.getMessage());
    } catch (DataAccessException ex) {
      res.setStatus(409);
      model.addAttribute(
          "message", "Đơn hàng chưa được xác nhận. Vui lòng kiểm tra lại hoặc thử lại.");
    }
    return page(quote, s, model);
  }

  private String page(CheckoutQuote quote, HttpSession s, Model model) {
    long total = 0;
    for (var l : quote.lines().values())
      total =
          com.chapter.bookstore.shared.Money.add(
              total, com.chapter.bookstore.shared.Money.multiply(l.price(), l.quantity()));
    model.addAttribute("quoteTotal", total);
    model.addAttribute("quote", quote);
    model.addAttribute("cartView", carts.view(CartService.cart(s)));
    return "checkout/form";
  }

  @GetMapping("/orders/{reference}/confirmation")
  String receipt(@PathVariable String reference, HttpSession s, Model model) {
    var state = (CheckoutSession) s.getAttribute("checkout");
    if (state == null) throw new WebProblem(404, "Không tìm thấy đơn hàng.");
    model.addAttribute("order", checkout.receipt(reference, state.ownerToken));
    return "checkout/confirmation";
  }
}

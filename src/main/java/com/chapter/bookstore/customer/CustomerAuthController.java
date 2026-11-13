package com.chapter.bookstore.customer;

import jakarta.servlet.http.*;
import java.beans.PropertyEditorSupport;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
public class CustomerAuthController {
  private final CustomerRegistrationService registration;
  private final CustomerAccountRepository accounts;

  public CustomerAuthController(
      CustomerRegistrationService registration, CustomerAccountRepository accounts) {
    this.registration = registration;
    this.accounts = accounts;
  }

  @InitBinder("registration")
  void bind(WebDataBinder binder) {
    binder.setAllowedFields("fullName", "email", "password", "confirmPassword");
    for (String field : new String[] {"password", "confirmPassword"})
      binder.registerCustomEditor(
          String.class,
          field,
          new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
              setValue(text);
            }
          });
  }

  @GetMapping("/register")
  String register(@RequestParam(required = false) String next, HttpSession session, Model model) {
    ContinueTarget.remember(session, next);
    model.addAttribute("registration", new RegistrationForm());
    model.addAttribute("fieldErrors", Map.of());
    return "customer/register";
  }

  @PostMapping("/register")
  String register(
      @ModelAttribute("registration") RegistrationForm form,
      BindingResult binding,
      HttpServletResponse response,
      Model model) {
    try {
      registration.register(form);
      response.setStatus(303);
      response.setHeader("Location", "/login?registered");
      return null;
    } catch (CustomerRegistrationService.Rejected ex) {
      response.setStatus(422);
      model.addAttribute("fieldErrors", ex.errors);
      model.addAttribute("registration", form.safeCopy());
      model.asMap().remove(BindingResult.MODEL_KEY_PREFIX + "registration");
      return "customer/register";
    } catch (org.springframework.dao.DataAccessException ex) {
      response.setStatus(503);
      model.addAttribute(
          "fieldErrors", Map.of("form", "Chưa thể tạo tài khoản. Vui lòng thử lại sau."));
      model.addAttribute("registration", form.safeCopy());
      model.asMap().remove(BindingResult.MODEL_KEY_PREFIX + "registration");
      return "customer/register";
    } finally {
      form.eraseSecrets();
    }
  }

  @GetMapping("/login")
  String login(
      @RequestParam(required = false) String next,
      HttpSession session,
      HttpServletRequest request,
      HttpServletResponse response,
      Model model) {
    ContinueTarget.remember(session, next);
    Object feedback = request.getAttribute("customerLoginRequestFeedback");
    if (feedback == null) feedback = session.getAttribute("customerLoginFeedback");
    session.removeAttribute("customerLoginFeedback");
    var f =
        feedback instanceof com.chapter.bookstore.customer.security.CustomerLoginFeedback value
            ? value
            : new com.chapter.bookstore.customer.security.CustomerLoginFeedback("", "");
    model.addAttribute("email", f.email());
    model.addAttribute("loginMessage", f.message());
    if (request.getAttribute("customerLoginStatus") instanceof Integer status)
      response.setStatus(status);
    return "customer/login";
  }

  @GetMapping("/account")
  String account(org.springframework.security.core.Authentication auth, Model model) {
    var p = (com.chapter.bookstore.customer.security.CustomerPrincipal) auth.getPrincipal();
    var a = accounts.findById(p.id).orElseThrow();
    model.addAttribute("profile", new CustomerProfileView(a.fullName, a.emailNormalized));
    return "customer/account";
  }

  @GetMapping("/logout")
  String logout(org.springframework.security.core.Authentication auth) {
    if (com.chapter.bookstore.security.AuthenticatedEntryGuard.hasRole(auth, "ADMIN"))
      return "redirect:/admin/books";
    if (!com.chapter.bookstore.security.AuthenticatedEntryGuard.hasRole(auth, "CUSTOMER"))
      return "redirect:/books";
    return "customer/logout";
  }

  @PostMapping("/logout")
  String adminLogout() {
    throw new com.chapter.bookstore.shared.WebProblem(403, "Vui lòng dùng đăng xuất quản trị.");
  }
}

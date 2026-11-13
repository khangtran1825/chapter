package com.chapter.bookstore.customer.security;

import com.chapter.bookstore.customer.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CustomerLoginAttemptGuard extends OncePerRequestFilter {
  private final CustomerIdentityPolicy identity;
  private final CustomerLoginAttempts attempts;

  public CustomerLoginAttemptGuard(
      CustomerIdentityPolicy identity, CustomerLoginAttempts attempts) {
    this.identity = identity;
    this.attempts = attempts;
  }

  public static String key(HttpServletRequest r) {
    String e = CustomerIdentityPolicy.normalizeEmail(r.getParameter("email"));
    return com.chapter.bookstore.checkout.ReceiptAccess.hash(
        e.length() + ":" + e + ":" + r.getRemoteAddr());
  }

  public static String safeEmail(HttpServletRequest r) {
    String e = CustomerIdentityPolicy.normalizeEmail(r.getParameter("email"));
    return e.substring(0, e.offsetByCodePoints(0, Math.min(254, e.codePointCount(0, e.length()))));
  }

  protected void doFilterInternal(HttpServletRequest r, HttpServletResponse s, FilterChain c)
      throws ServletException, IOException {
    if ("POST".equals(r.getMethod()) && (r.getContextPath() + "/login").equals(r.getRequestURI())) {
      if (!identity.validEmail(CustomerIdentityPolicy.normalizeEmail(r.getParameter("email")))
          || !CustomerPasswordPolicy.valid(r.getParameter("password"))) {
        render(r, s, 422, 0, "Vui lòng nhập email hợp lệ và mật khẩu từ 12 đến 64 ký tự.");
        return;
      }
      var d = attempts.permit(key(r));
      if (d.status() != 0) {
        render(
            r,
            s,
            d.status(),
            d.retryAfter(),
            "Tạm thời chưa thể đăng nhập. Vui lòng thử lại sau ít phút.");
        return;
      }
    }
    c.doFilter(r, s);
  }

  private void render(
      HttpServletRequest r, HttpServletResponse s, int status, long retry, String message)
      throws ServletException, IOException {
    s.setStatus(status);
    if (retry > 0) s.setHeader("Retry-After", Long.toString(retry));
    r.setAttribute("customerLoginStatus", status);
    r.setAttribute(
        "customerLoginRequestFeedback", new CustomerLoginFeedback(safeEmail(r), message));
    r.getRequestDispatcher("/login")
        .forward(
            new HttpServletRequestWrapper(r) {
              public String getMethod() {
                return "GET";
              }
            },
            s);
  }
}

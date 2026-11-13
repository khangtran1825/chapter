package com.chapter.bookstore.customer.security;

import jakarta.servlet.http.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomerLoginFailureHandler implements AuthenticationFailureHandler {
  private final CustomerLoginAttempts attempts;

  public CustomerLoginFailureHandler(CustomerLoginAttempts attempts) {
    this.attempts = attempts;
  }

  public void onAuthenticationFailure(
      HttpServletRequest r, HttpServletResponse s, AuthenticationException e) {
    attempts.failed(CustomerLoginAttemptGuard.key(r));
    r.getSession()
        .setAttribute(
            "customerLoginFeedback",
            new CustomerLoginFeedback(
                CustomerLoginAttemptGuard.safeEmail(r),
                "Email hoặc mật khẩu không đúng, hoặc tài khoản không thể đăng nhập."));
    s.setStatus(303);
    s.setHeader("Location", "/login?error");
  }
}

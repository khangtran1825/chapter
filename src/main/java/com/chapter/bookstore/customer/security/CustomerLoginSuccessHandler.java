package com.chapter.bookstore.customer.security;

import com.chapter.bookstore.customer.ContinueTarget;
import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomerLoginSuccessHandler implements AuthenticationSuccessHandler {
  private final CustomerLoginAttempts attempts;

  public CustomerLoginSuccessHandler(CustomerLoginAttempts attempts) {
    this.attempts = attempts;
  }

  public void onAuthenticationSuccess(
      HttpServletRequest r, HttpServletResponse s, Authentication auth) {
    attempts.succeeded(CustomerLoginAttemptGuard.key(r));
    var session = r.getSession();
    var allowed =
        Set.of(
            "SPRING_SECURITY_CONTEXT",
            "cart",
            "checkout",
            "deliveryDraft",
            "customerContinueTarget",
            "org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
    for (String name : Collections.list(session.getAttributeNames()))
      if (!allowed.contains(name)) session.removeAttribute(name);
    s.setStatus(303);
    s.setHeader("Location", ContinueTarget.consume(session));
  }
}

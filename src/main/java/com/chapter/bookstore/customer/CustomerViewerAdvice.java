package com.chapter.bookstore.customer;

import com.chapter.bookstore.customer.security.CustomerPrincipal;
import com.chapter.bookstore.security.AuthenticatedEntryGuard;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
public class CustomerViewerAdvice {
  public record Viewer(boolean customer, boolean admin, String displayName) {}

  @ModelAttribute("customerViewer")
  public Viewer viewer(Authentication a) {
    return new Viewer(
        a != null && a.getPrincipal() instanceof CustomerPrincipal,
        AuthenticatedEntryGuard.hasRole(a, "ADMIN"),
        a != null && a.getPrincipal() instanceof CustomerPrincipal p ? p.displayName : "");
  }
}

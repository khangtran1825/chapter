package com.chapter.bookstore.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Set;
import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class AuthenticatedEntryGuard extends OncePerRequestFilter {
  public static boolean hasRole(Authentication a, String role) {
    return a != null
        && a.isAuthenticated()
        && a.getAuthorities().stream().anyMatch(g -> g.getAuthority().equals("ROLE_" + role));
  }

  protected void doFilterInternal(HttpServletRequest r, HttpServletResponse s, FilterChain c)
      throws ServletException, IOException {
    var a = SecurityContextHolder.getContext().getAuthentication();
    if (Set.of("/login", "/register", "/admin/login")
            .contains(r.getServletPath().isEmpty() ? r.getRequestURI() : r.getServletPath())
        && (hasRole(a, "CUSTOMER") || hasRole(a, "ADMIN"))) {
      s.setStatus("POST".equals(r.getMethod()) ? 303 : 302);
      s.setHeader("Location", hasRole(a, "ADMIN") ? "/admin/books" : "/account");
      return;
    }
    c.doFilter(r, s);
  }
}

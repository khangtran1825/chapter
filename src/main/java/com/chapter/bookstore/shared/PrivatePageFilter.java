package com.chapter.bookstore.shared;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class PrivatePageFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String p = req.getRequestURI();
    if (p.equals("/register")
        || p.equals("/login")
        || p.startsWith("/account")
        || p.startsWith("/favorites")
        || p.equals("/logout")
        || p.startsWith("/admin")
        || p.startsWith("/orders")
        || p.startsWith("/checkout")
        || p.startsWith("/cart")) res.setHeader("Cache-Control", "no-store");
    chain.doFilter(req, res);
  }
}

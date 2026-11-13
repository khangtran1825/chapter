package com.chapter.bookstore.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.*;
import java.util.*;
import org.springframework.security.core.*;
import org.springframework.security.web.authentication.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class LoginAttemptGuard extends OncePerRequestFilter
    implements AuthenticationFailureHandler, AuthenticationSuccessHandler {
  private record Attempt(int count, Instant until) {}

  private final Map<String, Attempt> attempts = new LinkedHashMap<>();

  private String key(HttpServletRequest r) {
    String u = r.getParameter("username");
    String normalized = u == null ? "" : u.trim().toLowerCase(Locale.ROOT);
    return r.getRemoteAddr() + ":" + normalized.substring(0, Math.min(normalized.length(), 100));
  }

  private synchronized boolean blocked(String key) {
    var a = attempts.get(key);
    if (a == null) return false;
    if (a.until.isBefore(Instant.now())) {
      attempts.remove(key);
      return false;
    }
    return a.count >= 5;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest r, HttpServletResponse s, FilterChain c)
      throws ServletException, IOException {
    if ("POST".equals(r.getMethod())
        && (r.getContextPath() + "/admin/login").equals(r.getRequestURI())
        && blocked(key(r))) {
      s.setStatus(429);
      s.setContentType("text/plain;charset=UTF-8");
      s.getWriter().write("Tạm thời chưa thể đăng nhập. Vui lòng thử lại sau ít phút.");
      return;
    }
    c.doFilter(r, s);
  }

  @Override
  public synchronized void onAuthenticationFailure(
      HttpServletRequest r, HttpServletResponse s, AuthenticationException e) throws IOException {
    String key = key(r);
    var old = attempts.get(key);
    if (old != null && old.until.isBefore(Instant.now())) old = null;
    attempts.put(key, new Attempt(old == null ? 1 : old.count + 1, Instant.now().plusSeconds(300)));
    while (attempts.size() > 10000) attempts.remove(attempts.keySet().iterator().next());
    s.sendRedirect("/admin/login?error");
  }

  @Override
  public synchronized void onAuthenticationSuccess(
      HttpServletRequest r, HttpServletResponse s, Authentication a) throws IOException {
    attempts.remove(key(r));
    s.sendRedirect("/admin/books");
  }
}

package com.chapter.bookstore.security;

import com.chapter.bookstore.customer.security.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration
public class SecurityConfig {
  @Bean
  @Primary
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  FilterRegistrationBean<LoginAttemptGuard> guardRegistration(LoginAttemptGuard guard) {
    var r = new FilterRegistrationBean<>(guard);
    r.setEnabled(false);
    return r;
  }

  @Bean
  FilterRegistrationBean<CustomerLoginAttemptGuard> customerGuardRegistration(
      CustomerLoginAttemptGuard guard) {
    var r = new FilterRegistrationBean<>(guard);
    r.setEnabled(false);
    return r;
  }

  private ProviderManager manager(UserDetailsService users, PasswordEncoder encoder) {
    var p = new DaoAuthenticationProvider(users);
    p.setPasswordEncoder(encoder);
    var m = new ProviderManager(p);
    m.setEraseCredentialsAfterAuthentication(true);
    return m;
  }

  @Bean
  @Order(1)
  SecurityFilterChain adminSecurity(
      HttpSecurity http,
      LoginAttemptGuard guard,
      AdminDetailsService users,
      PasswordEncoder encoder)
      throws Exception {
    return http.securityMatcher("/admin/**")
        .authenticationManager(manager(users, encoder))
        .authorizeHttpRequests(
            a -> a.requestMatchers("/admin/login").permitAll().anyRequest().hasRole("ADMIN"))
        .addFilterBefore(guard, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(new AuthenticatedEntryGuard(), LoginAttemptGuard.class)
        .formLogin(
            f ->
                f.loginPage("/admin/login")
                    .loginProcessingUrl("/admin/login")
                    .successHandler(guard)
                    .failureHandler(guard)
                    .permitAll())
        .logout(
            l ->
                l.logoutRequestMatcher(
                        r ->
                            "POST".equals(r.getMethod())
                                && r.getRequestURI().equals("/admin/logout")
                                && AuthenticatedEntryGuard.hasRole(
                                    SecurityContextHolder.getContext().getAuthentication(),
                                    "ADMIN"))
                    .logoutSuccessUrl("/admin/login?logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID"))
        .sessionManagement(s -> s.sessionFixation(f -> f.migrateSession()))
        .build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain customerSecurity(
      HttpSecurity http,
      CustomerDetailsService users,
      @Qualifier("customerPasswordEncoder") PasswordEncoder encoder,
      CustomerLoginAttemptGuard guard,
      CustomerLoginSuccessHandler success,
      CustomerLoginFailureHandler failure)
      throws Exception {
    return http.authenticationManager(manager(users, encoder))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers("/account", "/account/**", "/favorites", "/favorites/**")
                    .hasRole("CUSTOMER")
                    .requestMatchers(org.springframework.http.HttpMethod.POST, "/books/*/favorite")
                    .hasRole("CUSTOMER")
                    .anyRequest()
                    .permitAll())
        .requestCache(c -> c.requestCache(new NullRequestCache()))
        .addFilterBefore(guard, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(new AuthenticatedEntryGuard(), CustomerLoginAttemptGuard.class)
        .formLogin(
            f ->
                f.loginPage("/login")
                    .loginProcessingUrl("/login")
                    .usernameParameter("email")
                    .successHandler(success)
                    .failureHandler(failure)
                    .permitAll())
        .logout(
            l ->
                l.logoutRequestMatcher(
                        r ->
                            "POST".equals(r.getMethod())
                                && r.getRequestURI().equals("/logout")
                                && !AuthenticatedEntryGuard.hasRole(
                                    SecurityContextHolder.getContext().getAuthentication(),
                                    "ADMIN"))
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessHandler(
                        (r, s, a) -> {
                          s.setStatus(303);
                          s.setHeader("Location", "/books?loggedOut");
                        }))
        .exceptionHandling(
            e ->
                e.accessDeniedHandler(
                        (r, s, x) -> {
                          s.setStatus(403);
                          s.setContentType("text/html;charset=UTF-8");
                          s.getWriter()
                              .write(
                                  "<!doctype html><html lang=vi><meta charset=utf-8><title>Không"
                                      + " thể tiếp tục</title><main><h1>Không thể tiếp tục yêu"
                                      + " cầu</h1><p>Phiên hoặc biểu mẫu không hợp lệ. Vui lòng tải"
                                      + " lại trang rồi thử lại.</p><a href=/login>Đăng nhập</a> ·"
                                      + " <a href=/books>Về cửa hàng</a></main></html>");
                        })
                    .authenticationEntryPoint(
                        (r, s, x) ->
                            s.sendRedirect(
                                "/login?next="
                                    + (r.getRequestURI().startsWith("/favorites")
                                            || r.getRequestURI().matches("/books/[^/]+/favorite")
                                        ? "favorites"
                                        : "account")
                                    + (r.getRequestedSessionId() != null
                                            && !r.isRequestedSessionIdValid()
                                        ? "&expired"
                                        : ""))))
        .sessionManagement(s -> s.sessionFixation(f -> f.migrateSession()))
        .build();
  }
}

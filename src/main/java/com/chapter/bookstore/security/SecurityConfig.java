package com.chapter.bookstore.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  FilterRegistrationBean<LoginAttemptGuard> guardRegistration(LoginAttemptGuard guard) {
    var registration = new FilterRegistrationBean<>(guard);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http, LoginAttemptGuard guard) throws Exception {
    return http.authorizeHttpRequests(a ->
            a.requestMatchers("/admin/login", "/css/**", "/images/**", "/error")
                .permitAll()
                .requestMatchers("/admin/**")
                .hasRole("ADMIN")
                .anyRequest()
                .permitAll())
        .addFilterBefore(guard, UsernamePasswordAuthenticationFilter.class)
        .formLogin(f ->
            f.loginPage("/admin/login")
                .loginProcessingUrl("/admin/login")
                .successHandler(guard)
                .failureHandler(guard)
                .permitAll())
        .logout(l ->
            l.logoutUrl("/admin/logout")
                .logoutSuccessUrl("/admin/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID"))
        .sessionManagement(s -> s.sessionFixation(f -> f.migrateSession()))
        .build();
  }
}

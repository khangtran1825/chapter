package com.chapter.bookstore.customer.security;

import java.time.Clock;
import java.util.Map;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.*;

@Configuration
public class CustomerSecurityBeans {
  @Bean("customerPasswordEncoder")
  PasswordEncoder customerPasswordEncoder() {
    return new DelegatingPasswordEncoder(
        "pbkdf2@SpringSecurity_v5_8",
        Map.of(
            "pbkdf2@SpringSecurity_v5_8", Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()));
  }

  @Bean("customerClock")
  Clock customerClock() {
    return Clock.systemUTC();
  }

  @Bean
  CustomerLoginAttempts customerLoginAttempts(
      @org.springframework.beans.factory.annotation.Qualifier("customerClock") Clock clock) {
    return new CustomerLoginAttempts(clock, 10000);
  }
}

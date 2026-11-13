package com.chapter.bookstore.security;

import java.util.Locale;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class AdminDetailsService implements UserDetailsService {
  private final AdminUserRepository users;

  public AdminDetailsService(AdminUserRepository users) {
    this.users = users;
  }

  @Override
  public UserDetails loadUserByUsername(String input) {
    var u =
        users
            .findByUsername(input.trim().toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new UsernameNotFoundException("Thông tin đăng nhập không đúng"));
    return User.withUsername(u.username)
        .password(u.passwordHash)
        .roles(u.role)
        .disabled(!u.enabled)
        .build();
  }
}

package com.chapter.bookstore.customer.security;

import com.chapter.bookstore.customer.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomerDetailsService implements UserDetailsService {
  private final CustomerAccountRepository accounts;

  public CustomerDetailsService(CustomerAccountRepository accounts) {
    this.accounts = accounts;
  }

  public UserDetails loadUserByUsername(String email) {
    return new CustomerPrincipal(
        accounts
            .findByEmailNormalized(CustomerIdentityPolicy.normalizeEmail(email))
            .orElseThrow(() -> new UsernameNotFoundException("Thông tin đăng nhập không hợp lệ.")));
  }
}

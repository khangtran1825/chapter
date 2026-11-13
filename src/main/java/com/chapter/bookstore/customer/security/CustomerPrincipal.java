package com.chapter.bookstore.customer.security;

import com.chapter.bookstore.customer.CustomerAccount;
import java.util.*;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class CustomerPrincipal implements UserDetails, CredentialsContainer {
  public final UUID id;
  public final String displayName;
  private final String email;
  private final boolean enabled;
  private transient String hash;

  public CustomerPrincipal(CustomerAccount a) {
    id = a.id;
    displayName = a.fullName;
    email = a.emailNormalized;
    enabled = a.enabled;
    hash = a.passwordHash;
  }

  public String getUsername() {
    return email;
  }

  public String getPassword() {
    return hash;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
  }

  public void eraseCredentials() {
    hash = null;
  }
}

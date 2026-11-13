package com.chapter.bookstore.customer;

import java.time.Clock;
import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CustomerRegistrationService {
  private final CustomerAccountRepository accounts;
  private final CustomerIdentityPolicy identity;
  private final PasswordEncoder encoder;
  private final Clock clock;

  public CustomerRegistrationService(
      CustomerAccountRepository accounts,
      CustomerIdentityPolicy identity,
      @Qualifier("customerPasswordEncoder") PasswordEncoder encoder,
      @Qualifier("customerClock") Clock clock) {
    this.accounts = accounts;
    this.identity = identity;
    this.encoder = encoder;
    this.clock = clock;
  }

  public Map<String, String> errors(RegistrationForm f) {
    Map<String, String> errors = new LinkedHashMap<>();
    if (!CustomerIdentityPolicy.validName(f.fullName))
      errors.put("fullName", "Họ tên phải từ 1 đến 100 ký tự.");
    if (!identity.validEmail(CustomerIdentityPolicy.normalizeEmail(f.email)))
      errors.put("email", "Vui lòng nhập email hợp lệ, tối đa 254 ký tự.");
    if (!CustomerPasswordPolicy.valid(f.password))
      errors.put("password", "Mật khẩu phải từ 12 đến 64 ký tự.");
    if (!Objects.equals(f.password, f.confirmPassword))
      errors.put("confirmPassword", "Xác nhận mật khẩu chưa khớp.");
    return errors;
  }

  // Repository saveAndFlush owns its transaction; exceptions are translated after rollback.
  public UUID register(RegistrationForm f) {
    var invalid = errors(f);
    if (!invalid.isEmpty()) throw new Rejected(invalid);
    String email = CustomerIdentityPolicy.normalizeEmail(f.email);
    var account = new CustomerAccount();
    account.fullName = CustomerIdentityPolicy.normalizeName(f.fullName);
    account.emailNormalized = email;
    account.passwordHash = encoder.encode(f.password);
    account.createdAt = clock.instant();
    try {
      return accounts.saveAndFlush(account).id;
    } catch (DataIntegrityViolationException ex) {
      if (accounts.existsByEmailNormalized(email))
        throw new Rejected(Map.of("email", "Email này đã được đăng ký. Bạn có thể đăng nhập."));
      throw ex;
    }
  }

  public static final class Rejected extends RuntimeException {
    public final Map<String, String> errors;

    public Rejected(Map<String, String> errors) {
      super("Thông tin đăng ký không hợp lệ.");
      this.errors = Map.copyOf(errors);
    }
  }
}

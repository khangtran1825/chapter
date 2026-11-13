package com.chapter.bookstore.customer;

/** Never persist this form or its rejected secret values in session or flash. */
public class RegistrationForm {
  public String fullName = "";
  public String email = "";
  public String password = "";
  public String confirmPassword = "";

  public RegistrationForm safeCopy() {
    var safe = new RegistrationForm();
    safe.fullName = CustomerIdentityPolicy.normalizeName(fullName);
    safe.email = CustomerIdentityPolicy.normalizeEmail(email);
    return safe;
  }

  public void eraseSecrets() {
    password = "";
    confirmPassword = "";
  }
}

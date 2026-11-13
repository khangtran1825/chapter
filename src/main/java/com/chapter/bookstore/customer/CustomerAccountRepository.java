package com.chapter.bookstore.customer;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerAccountRepository extends JpaRepository<CustomerAccount, UUID> {
  Optional<CustomerAccount> findByEmailNormalized(String email);

  boolean existsByEmailNormalized(String email);
}

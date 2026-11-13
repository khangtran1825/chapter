package com.chapter.bookstore.order;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface OrderRepository extends JpaRepository<CustomerOrder, UUID> {
  Optional<CustomerOrder> findByCheckoutKey(UUID key);

  Optional<CustomerOrder> findByReference(String reference);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from CustomerOrder o where o.id=:id")
  Optional<CustomerOrder> locked(UUID id);

  Page<CustomerOrder> findByStatus(OrderStatus status, Pageable pageable);
}

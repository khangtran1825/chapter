package com.chapter.bookstore.catalog;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, UUID> {
  @Query(
      "select b from Book b where b.active=true and (locate(lower(:q),lower(b.title))>0 or"
          + " locate(lower(:q),lower(b.author))>0) and (:category is null or"
          + " b.category.id=:category)")
  Page<Book> search(@Param("q") String q, @Param("category") UUID category, Pageable pageable);

  boolean existsByCategoryId(UUID id);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select b from Book b where b.id=:id")
  Optional<Book> locked(UUID id);
}

package com.chapter.bookstore.catalog;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
  List<Category> findAllByOrderByNameAsc();
}

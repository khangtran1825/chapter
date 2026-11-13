package com.chapter.bookstore.catalog;

import com.chapter.bookstore.shared.WebProblem;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogService {
  private final BookRepository books;

  public CatalogService(BookRepository books) {
    this.books = books;
  }

  public Page<Book> search(String q, UUID category, int page) {
    if (q.length() > 200 || page < 0 || page > 100000)
      throw new WebProblem(400, "Từ khóa hoặc số trang không hợp lệ.");
    return books.search(q.trim(), category, PageRequest.of(page, 12, Sort.by("title", "id")));
  }

  public Book book(UUID id) {
    Book b = books.findById(id).orElseThrow(() -> new WebProblem(404, "Không tìm thấy sách."));
    if (!b.active) throw new WebProblem(404, "Sách không còn được bày bán.");
    return b;
  }
}

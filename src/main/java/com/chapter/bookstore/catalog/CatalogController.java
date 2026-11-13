package com.chapter.bookstore.catalog;

import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class CatalogController {
  private final CatalogService catalog;
  private final CategoryRepository categories;

  public CatalogController(CatalogService catalog, CategoryRepository categories) {
    this.catalog = catalog;
    this.categories = categories;
  }

  @GetMapping("/books")
  String list(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(required = false) UUID category,
      @RequestParam(defaultValue = "0") int page,
      Model model) {
    q = q == null ? "" : q;
    var result = catalog.search(q, category, page);
    model.addAttribute("books", result);
    model.addAttribute("categories", categories.findAllByOrderByNameAsc());
    model.addAttribute("q", q.trim());
    model.addAttribute("category", category);
    return "catalog/list";
  }

  @GetMapping("/books/{id}")
  String detail(@PathVariable UUID id, Model model) {
    model.addAttribute("book", catalog.book(id));
    return "catalog/detail";
  }
}

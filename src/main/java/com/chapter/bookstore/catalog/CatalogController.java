package com.chapter.bookstore.catalog;

import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class CatalogController {
  private final CatalogService catalog;
  private final CategoryRepository categories;
  private final com.chapter.bookstore.favorites.FavoriteService favorites;

  public CatalogController(
      CatalogService catalog,
      CategoryRepository categories,
      com.chapter.bookstore.favorites.FavoriteService favorites) {
    this.catalog = catalog;
    this.categories = categories;
    this.favorites = favorites;
  }

  @GetMapping("/books")
  String list(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(required = false) UUID category,
      @RequestParam(defaultValue = "0") int page,
      Model model,
      org.springframework.security.core.Authentication auth) {
    q = q == null ? "" : q;
    var result = catalog.search(q, category, page);
    model.addAttribute("books", result);
    model.addAttribute("categories", categories.findAllByOrderByNameAsc());
    model.addAttribute("q", q.trim());
    model.addAttribute("category", category);
    model.addAttribute(
        "favoriteIds", saved(auth, result.getContent().stream().map(b -> b.id).toList()));
    return "catalog/list";
  }

  @GetMapping("/books/{id}")
  String detail(
      @PathVariable UUID id, Model model, org.springframework.security.core.Authentication auth) {
    model.addAttribute("book", catalog.book(id));
    model.addAttribute("favoriteIds", saved(auth, List.of(id)));
    return "catalog/detail";
  }

  private Set<UUID> saved(
      org.springframework.security.core.Authentication auth, Collection<UUID> ids) {
    return auth != null
            && auth.getPrincipal()
                instanceof com.chapter.bookstore.customer.security.CustomerPrincipal p
        ? favorites.savedIds(p.id, ids)
        : Set.of();
  }
}

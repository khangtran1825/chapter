package com.chapter.bookstore.admin;

import com.chapter.bookstore.catalog.*;
import com.chapter.bookstore.shared.WebProblem;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class AdminBookController {
  private final BookRepository books;
  private final CategoryRepository categories;
  private final AdminCatalogService service;

  public AdminBookController(BookRepository b, CategoryRepository c, AdminCatalogService s) {
    books = b;
    categories = c;
    service = s;
  }

  @GetMapping("/admin/books")
  String list(@RequestParam(defaultValue = "0") int page, Model model) {
    if (page < 0 || page > 100000) throw new WebProblem(400, "Số trang không hợp lệ.");
    model.addAttribute("books", books.findAll(PageRequest.of(page, 20, Sort.by("title", "id"))));
    return "admin/books";
  }

  @GetMapping("/admin/books/new")
  String create(Model m) {
    m.addAttribute("bookForm", new BookForm());
    return form(null, m);
  }

  @GetMapping("/admin/books/{id}/edit")
  String edit(@PathVariable UUID id, Model m) {
    m.addAttribute(
        "bookForm",
        BookForm.from(
            books.findById(id).orElseThrow(() -> new WebProblem(404, "Không tìm thấy sách."))));
    return form(id, m);
  }

  @PostMapping({"/admin/books", "/admin/books/{id}"})
  String save(
      @PathVariable(required = false) UUID id,
      @Valid @ModelAttribute("bookForm") BookForm f,
      BindingResult errors,
      Model m,
      HttpServletResponse res) {
    if (errors.hasErrors()) {
      res.setStatus(422);
      return form(id, m);
    }
    try {
      service.save(id, f);
    } catch (WebProblem ex) {
      res.setStatus(ex.status);
      m.addAttribute("message", ex.getMessage());
      return form(id, m);
    } catch (DataAccessException ex) {
      res.setStatus(409);
      m.addAttribute("message", "Dữ liệu vừa thay đổi. Vui lòng kiểm tra lại.");
      return form(id, m);
    }
    return redirect(res);
  }

  private String form(UUID id, Model m) {
    m.addAttribute("bookId", id);
    m.addAttribute("categories", categories.findAllByOrderByNameAsc());
    return "admin/book-form";
  }

  @PostMapping("/admin/books/{id}/inventory")
  String stock(
      @PathVariable UUID id,
      @RequestParam int stock,
      @RequestParam long version,
      HttpServletResponse res) {
    service.inventory(id, stock, version);
    return redirect(res);
  }

  @PostMapping("/admin/books/{id}/archive")
  String archive(@PathVariable UUID id, @RequestParam long version, HttpServletResponse res) {
    service.active(id, false, version);
    return redirect(res);
  }

  @PostMapping("/admin/books/{id}/activate")
  String activate(@PathVariable UUID id, @RequestParam long version, HttpServletResponse res) {
    service.active(id, true, version);
    return redirect(res);
  }

  private String redirect(HttpServletResponse res) {
    res.setStatus(303);
    res.setHeader("Location", "/admin/books");
    return null;
  }
}

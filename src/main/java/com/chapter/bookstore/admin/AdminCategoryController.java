package com.chapter.bookstore.admin;

import com.chapter.bookstore.catalog.CategoryRepository;
import com.chapter.bookstore.shared.WebProblem;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AdminCategoryController {
  private final CategoryRepository categories;
  private final AdminCatalogService service;

  public AdminCategoryController(CategoryRepository c, AdminCatalogService s) {
    categories = c;
    service = s;
  }

  @GetMapping("/admin/categories")
  String list(Model m) {
    m.addAttribute("categories", categories.findAllByOrderByNameAsc());
    return "admin/categories";
  }

  @PostMapping({"/admin/categories", "/admin/categories/{id}"})
  String save(
      @PathVariable(required = false) UUID id,
      @RequestParam String name,
      Model m,
      HttpServletResponse res) {
    try {
      service.category(id, name);
      return redirect(res);
    } catch (WebProblem ex) {
      res.setStatus(ex.status);
      m.addAttribute("message", ex.getMessage());
    } catch (DataAccessException ex) {
      res.setStatus(409);
      m.addAttribute("message", "Tên thể loại đã được dùng. Vui lòng chọn tên khác.");
    }
    m.addAttribute("name", name);
    m.addAttribute("renameId", id);
    return list(m);
  }

  @PostMapping("/admin/categories/{id}/delete")
  String delete(@PathVariable UUID id, HttpServletResponse res) {
    service.deleteCategory(id);
    return redirect(res);
  }

  private String redirect(HttpServletResponse res) {
    res.setStatus(303);
    res.setHeader("Location", "/admin/categories");
    return null;
  }
}

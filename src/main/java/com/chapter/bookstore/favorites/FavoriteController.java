package com.chapter.bookstore.favorites;

import com.chapter.bookstore.customer.security.CustomerPrincipal;
import com.chapter.bookstore.shared.WebProblem;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class FavoriteController {
  private final FavoriteService favorites;

  public FavoriteController(FavoriteService favorites) {
    this.favorites = favorites;
  }

  private UUID owner(Authentication auth) {
    if (auth == null || !(auth.getPrincipal() instanceof CustomerPrincipal p))
      throw new WebProblem(403, "Vui lòng đăng nhập tài khoản khách hàng.");
    return p.id;
  }

  @PostMapping("/books/{id}/favorite")
  void add(@PathVariable UUID id, Authentication auth, HttpServletResponse response) {
    favorites.add(owner(auth), id);
    response.setStatus(303);
    response.setHeader("Location", "/books/" + id + "?favoriteSaved");
  }

  @GetMapping("/favorites")
  String list(
      @RequestParam(defaultValue = "0") int page,
      Authentication auth,
      org.springframework.ui.Model model) {
    model.addAttribute("books", favorites.list(owner(auth), page));
    return "favorites/list";
  }

  @PostMapping("/favorites/{id}/remove")
  void remove(@PathVariable UUID id, Authentication auth, HttpServletResponse response) {
    favorites.remove(owner(auth), id);
    response.setStatus(303);
    response.setHeader("Location", "/favorites");
  }

  @ExceptionHandler(DataAccessException.class)
  ModelAndView unavailable() {
    var view = new ModelAndView("error/error");
    view.setStatus(HttpStatus.SERVICE_UNAVAILABLE);
    view.addObject("status", 503);
    view.addObject("message", "Chưa thể cập nhật hoặc tải sách yêu thích. Vui lòng thử lại sau.");
    return view;
  }
}

package com.chapter.bookstore.shared;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NavigationController {
  @GetMapping("/")
  String home() {
    return "redirect:/books";
  }

  @GetMapping("/admin/login")
  String login() {
    return "admin/login";
  }
}

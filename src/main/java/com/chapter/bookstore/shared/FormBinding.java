package com.chapter.bookstore.shared;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
public class FormBinding {
  @InitBinder
  void bind(WebDataBinder binder) {
    binder.initDirectFieldAccess();
    binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
  }
}

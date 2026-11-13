package com.chapter.bookstore.shared;

import org.springframework.dao.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class WebErrorHandler {
  @ExceptionHandler(WebProblem.class)
  ModelAndView problem(WebProblem ex) {
    return page(ex.status, ex.getMessage());
  }

  @ExceptionHandler({
    DataIntegrityViolationException.class,
    OptimisticLockingFailureException.class,
    PessimisticLockingFailureException.class
  })
  ModelAndView conflict(Exception ex) {
    return page(409, "Dữ liệu vừa thay đổi hoặc đang được sử dụng. Vui lòng tải lại và thử lại.");
  }

  @ExceptionHandler({
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class,
    IllegalArgumentException.class
  })
  ModelAndView invalid(Exception ex) {
    return page(400, "Thông tin gửi lên không hợp lệ. Vui lòng kiểm tra và thử lại.");
  }

  private ModelAndView page(int status, String message) {
    ModelAndView view = new ModelAndView("error/error");
    view.setStatus(HttpStatus.valueOf(status));
    view.addObject("message", message);
    view.addObject("status", status);
    return view;
  }
}

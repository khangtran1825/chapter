package com.chapter.bookstore.shared;

public class WebProblem extends RuntimeException {
  public final int status;

  public WebProblem(int status, String message) {
    super(message);
    this.status = status;
  }
}

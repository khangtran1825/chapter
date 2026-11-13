package com.chapter.bookstore.admin;

import com.chapter.bookstore.catalog.Book;
import jakarta.validation.constraints.*;
import java.util.UUID;

public class BookForm {
  @NotBlank
  @Size(max = 255)
  public String title = "";

  @NotBlank
  @Size(max = 255)
  public String author = "";

  @NotBlank
  @Size(max = 10000)
  public String description = "";

  @Pattern(regexp = "/images/[a-zA-Z0-9._-]+\\.(svg|png|jpg|jpeg|webp)")
  @Size(max = 255)
  public String coverPath;

  @NotNull
  @Min(0)
  public Long priceVnd = 0L;

  @NotNull
  @Min(0)
  public Integer stock = 0;

  @NotNull public UUID categoryId;

  @NotNull
  @Min(0)
  public Long version = 0L;

  public static BookForm from(Book b) {
    var f = new BookForm();
    f.title = b.title;
    f.author = b.author;
    f.description = b.description;
    f.coverPath = b.coverPath;
    f.priceVnd = b.priceVnd;
    f.stock = b.stock;
    f.categoryId = b.category.id;
    f.version = b.version;
    return f;
  }
}

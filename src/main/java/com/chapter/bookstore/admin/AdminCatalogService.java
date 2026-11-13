package com.chapter.bookstore.admin;

import com.chapter.bookstore.catalog.*;
import com.chapter.bookstore.shared.WebProblem;
import jakarta.validation.Validator;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCatalogService {
  private final BookRepository books;
  private final CategoryRepository categories;
  private final Validator validator;

  public AdminCatalogService(BookRepository b, CategoryRepository c, Validator v) {
    books = b;
    categories = c;
    validator = v;
  }

  @Transactional
  public Book save(UUID id, BookForm f) {
    if (!validator.validate(f).isEmpty()) throw new WebProblem(422, "Thông tin sách không hợp lệ.");
    Book b = id == null ? new Book() : locked(id);
    if (id != null && b.version != f.version)
      throw new WebProblem(409, "Sách vừa thay đổi. Vui lòng tải lại trước khi sửa.");
    b.category =
        categories
            .findById(f.categoryId)
            .orElseThrow(() -> new WebProblem(422, "Thể loại không tồn tại."));
    b.title = f.title.trim();
    b.author = f.author.trim();
    b.description = f.description.trim();
    b.coverPath = f.coverPath;
    b.priceVnd = f.priceVnd;
    b.stock = f.stock;
    return books.saveAndFlush(b);
  }

  private Book locked(UUID id) {
    return books.locked(id).orElseThrow(() -> new WebProblem(404, "Không tìm thấy sách."));
  }

  @Transactional
  public void inventory(UUID id, int stock, long version) {
    if (stock < 0) throw new WebProblem(422, "Tồn kho không được âm.");
    var b = locked(id);
    if (b.version != version) throw new WebProblem(409, "Tồn kho vừa thay đổi. Vui lòng tải lại.");
    b.stock = stock;
    books.flush();
  }

  @Transactional
  public void active(UUID id, boolean active, long version) {
    var b = locked(id);
    if (b.version != version) throw new WebProblem(409, "Sách vừa thay đổi. Vui lòng tải lại.");
    b.active = active;
    books.flush();
  }

  @Transactional
  public void category(UUID id, String name) {
    if (name == null || name.isBlank() || name.length() > 100)
      throw new WebProblem(422, "Tên thể loại phải từ 1 đến 100 ký tự.");
    var c =
        id == null
            ? new Category()
            : categories
                .findById(id)
                .orElseThrow(() -> new WebProblem(404, "Không tìm thấy thể loại."));
    c.name = name.trim();
    categories.saveAndFlush(c);
  }

  @Transactional
  public void deleteCategory(UUID id) {
    var c =
        categories.findById(id).orElseThrow(() -> new WebProblem(404, "Không tìm thấy thể loại."));
    if (books.existsByCategoryId(id))
      throw new WebProblem(409, "Hãy chuyển sách sang thể loại khác trước khi xóa.");
    categories.delete(c);
    categories.flush();
  }
}

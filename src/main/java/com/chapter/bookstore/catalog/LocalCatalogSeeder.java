package com.chapter.bookstore.catalog;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
public class LocalCatalogSeeder implements CommandLineRunner {
  private final BookRepository books;
  private final CategoryRepository categories;

  public LocalCatalogSeeder(BookRepository b, CategoryRepository c) {
    books = b;
    categories = c;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (books.count() != 0 || categories.count() != 0) return;
    Category[] groups = {
      new Category("Văn học"),
      new Category("Sống chậm"),
      new Category("Nghệ thuật"),
      new Category("Khám phá")
    };
    for (var c : groups) categories.save(c);
    String[] titles = {
      "Những ngày xanh trở lại",
      "Ở một nơi rất yên",
      "Đi qua mùa gió",
      "Một tách trà, một trang sách",
      "Bản đồ của những giấc mơ",
      "Chuyện kể dưới tán cây",
      "Gửi người đang lớn",
      "Sắc màu của im lặng",
      "Thành phố và những vì sao",
      "Đi tìm một ngày đẹp trời",
      "Người gom ánh sáng",
      "Những cuộc dạo chơi",
      "Bên kia ô cửa",
      "Nghệ thuật của sự chú tâm",
      "Hành trình không vội",
      "Khu vườn trong tâm trí"
    };
    String[] colors = {"green", "orange", "blue", "cream"};
    for (int i = 0; i < titles.length; i++) {
      Book b = new Book();
      b.title = titles[i];
      b.author = new String[] {"An Nhiên", "Minh Hà", "Lâm Phong", "Thu Giang"}[i % 4];
      b.description =
          "Một câu chuyện hư cấu dành cho những ai muốn dừng lại, lắng nghe và tìm thấy cảm hứng"
              + " trong những điều bình dị.\n\n"
              + "Ấn bản minh họa của Chapter. Dữ liệu mẫu dùng để trải nghiệm cửa hàng, không phải"
              + " sản phẩm đang bán thực tế.";
      b.category = groups[i % 4];
      b.priceVnd = 89000 + (i % 5) * 20000;
      b.stock = i == 15 ? 0 : 15;
      b.coverPath = "/images/cover-" + colors[i % 4] + ".svg";
      books.save(b);
    }
  }
}

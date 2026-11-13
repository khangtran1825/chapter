package com.chapter.bookstore.browser;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.*;

import com.chapter.bookstore.integration.Fixtures;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import org.junit.jupiter.api.Test;

class StorefrontBrowserIT extends BrowserTestSupport {
  @Test
  void completeShopperJourneyAtAllWidths() {
    for (int width : new int[] {360, 768, 1440}) {
      jdbc.update("update book set stock=3 where id=?", Fixtures.BOOK);
      page.setViewportSize(width, 1000);
      page.navigate(url("/books"));
      noOverflow();
      screenshot("catalog-" + width);
      page.getByLabel("Bạn muốn đọc gì hôm nay?").focus();
      page.keyboard().type("Những ngày xanh");
      page.keyboard().press("Enter");
      assertThat(page.locator(".book")).hasCount(1);
      page.getByRole(
              AriaRole.LINK, new Page.GetByRoleOptions().setName("Những ngày xanh").setExact(true))
          .click();
      noOverflow();
      page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Thêm vào giỏ sách"))
          .focus();
      page.keyboard().press("Enter");
      page.waitForURL("**/cart");
      assertThat(page.locator(".cart-line")).hasCount(1);
      page.getByLabel("Số lượng").fill("2");
      page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Cập nhật")).click();
      assertThat(page.locator(".summary")).containsText("250.000");
      page.reload();
      assertThat(page.locator(".cart-line")).hasCount(1);
      noOverflow();
      page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Tiến hành đặt sách"))
          .click();
      delivery();
      noOverflow();
      screenshot("checkout-" + width);
      if (width == 360) {
        page.locator("form.split").evaluate("form => form.noValidate=true");
        page.getByLabel("Số điện thoại").fill("123");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Xác nhận đặt sách"))
            .click();
        assertThat(page.getByRole(AriaRole.ALERT)).isVisible();
        assertThat(page.getByLabel("Họ và tên")).hasValue("Người đọc thử nghiệm");
        page.getByLabel("Số điện thoại").fill("0901234567");
      }
      page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Xác nhận đặt sách"))
          .focus();
      page.keyboard().press("Enter");
      page.waitForURL("**/confirmation");
      assertThat(page.locator("main")).containsText("Một chương mới đã bắt đầu");
      noOverflow();
      screenshot("confirmation-" + width);
      String receipt = page.url();
      try (var other = browser.newContext()) {
        var anonymous = other.newPage();
        org.assertj.core.api.Assertions.assertThat(anonymous.navigate(receipt).status())
            .isEqualTo(404);
        org.assertj.core.api.Assertions.assertThat(anonymous.content())
            .doesNotContain("0901234567");
      }
    }
  }

  @Test
  void emptyStatesMissingImagesAndLongTitlesAreUsable() {
    jdbc.update(
        "update book set title=?,cover_path='/images/missing.svg' where id=?",
        "Một cuốn sách có tên rất dài ".repeat(6),
        Fixtures.BOOK);
    page.setViewportSize(360, 900);
    page.navigate(url("/books/" + Fixtures.BOOK));
    assertThat(page.locator(".cover img")).hasAttribute("src", "/images/book-placeholder.svg");
    noOverflow();
    page.navigate(url("/books?q=not-found"));
    assertThat(page.locator(".empty")).containsText("Chưa tìm thấy");
    page.navigate(url("/cart"));
    assertThat(page.locator("main")).containsText("Giỏ sách đang trống");
    noOverflow();
  }
}

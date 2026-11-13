package com.chapter.bookstore.browser;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.chapter.bookstore.integration.Fixtures;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AdminBrowserIT extends BrowserTestSupport {
  @Test
  void staffCanManageCatalogInventoryAndOrderWithoutLeakingPrivateData() {
    page.navigate(url("/books/" + Fixtures.BOOK));
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Thêm vào giỏ sách"))
        .click();
    page.waitForURL("**/cart");
    String oldSession =
        context.cookies().stream()
            .filter(c -> c.name.equals("JSESSIONID"))
            .findFirst()
            .orElseThrow()
            .value;
    login();
    String newSession =
        context.cookies().stream()
            .filter(c -> c.name.equals("JSESSIONID"))
            .findFirst()
            .orElseThrow()
            .value;
    org.assertj.core.api.Assertions.assertThat(newSession).isNotEqualTo(oldSession);
    page.navigate(url("/cart"));
    assertThat(page.locator(".cart-line")).hasCount(1);
    page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Tiến hành đặt sách"))
        .click();
    delivery();
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Xác nhận đặt sách"))
        .click();
    page.waitForURL("**/confirmation");
    UUID order = jdbc.queryForObject("select id from customer_order limit 1", UUID.class);
    page.navigate(url("/admin/orders/" + order));
    page.getByRole(
            AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Đã xác nhận").setExact(true))
        .click();
    page.waitForURL("**/admin/orders/" + order);
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Đã hủy").setExact(true))
        .click();
    assertThat(page.locator("aside h2")).hasText("Đã hủy");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject("select stock from book where id=?", Integer.class, Fixtures.BOOK))
        .isEqualTo(3);
    page.navigate(url("/admin/books/new"));
    page.getByLabel("Tên sách").fill("<script>alert('x')</script>");
    page.getByLabel("Tác giả", new Page.GetByLabelOptions().setExact(true)).fill("Người viết mẫu");
    page.getByLabel("Giới thiệu sách").fill("Một câu chuyện hư cấu.");
    page.getByLabel("Thể loại", new Page.GetByLabelOptions().setExact(true))
        .selectOption(Fixtures.CAT.toString());
    page.getByLabel("Giá (₫)").fill("99000");
    page.getByLabel("Tồn kho", new Page.GetByLabelOptions().setExact(true)).fill("5");
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Lưu sách")).click();
    page.waitForURL("**/admin/books");
    assertThat(
            page.getByRole(
                AriaRole.LINK,
                new Page.GetByRoleOptions().setName("<script>alert('x')</script>").setExact(true)))
        .isVisible();
    page.navigate(url("/admin/categories"));
    page.getByLabel("Thêm thể loại").fill("Thể loại mới");
    page.getByRole(
            AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Thêm thể loại").setExact(true))
        .click();
    page.waitForURL("**/admin/categories");
    assertThat(page.locator("main input[value='Thể loại mới']")).isVisible();
    page.setViewportSize(360, 900);
    noOverflow();
    screenshot("admin-mobile");
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Đăng xuất")).click();
    page.waitForURL("**/admin/login?logout");
    page.navigate(url("/admin/orders"));
    assertThat(page).hasURL(url("/admin/login"));
    page.navigate(url("/books"));
    org.assertj.core.api.Assertions.assertThat(page.content())
        .doesNotContain("0901234567", "12 Đường Sách");
  }
}

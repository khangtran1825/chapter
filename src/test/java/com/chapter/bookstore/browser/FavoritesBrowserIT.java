package com.chapter.bookstore.browser;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.chapter.bookstore.integration.*;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

class FavoritesBrowserIT extends BrowserTestSupport {
  @Autowired
  @Qualifier("customerPasswordEncoder")
  PasswordEncoder encoder;

  String account() {
    String email = "favorite-" + UUID.randomUUID() + "@example.com";
    CustomerFixtures.create(jdbc, encoder, email, true);
    return email;
  }

  void loginCustomer(Page p, String email) {
    loginCustomer(p, email, "/books");
  }

  void loginCustomer(Page p, String email, String destination) {
    p.navigate(url("/login"));
    p.locator("[name=email]").fill(email);
    p.locator("[name=password]").fill(CustomerFixtures.PASSWORD);
    p.locator("button").click();
    p.waitForURL(url(destination));
  }

  void save(Page p) {
    p.navigate(
        url(
            "/books?q="
                + java.net.URLEncoder.encode(
                    "Những ngày xanh", java.nio.charset.StandardCharsets.UTF_8)));
    p.getByRole(
            AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Yêu thích Những ngày xanh").setExact(true))
        .focus();
    p.keyboard().press("Enter");
    p.waitForURL("**/books/" + Fixtures.BOOK + "?favoriteSaved");
    assertThat(p.locator("main")).containsText("Đã yêu thích");
  }

  @Test
  void saveListRemoveByKeyboardAtAllWidths() {
    String email = account();
    loginCustomer(page, email);
    jdbc.update("update book set stock=0 where id=?", Fixtures.BOOK);
    for (int width : new int[] {360, 768, 1440}) {
      page.setViewportSize(width, 1000);
      save(page);
      noOverflow();
      page.locator("header a[href='/favorites']").click();
      assertThat(page.locator("article.book")).hasCount(1);
      assertThat(page.locator("main")).containsText("Tạm hết sách");
      noOverflow();
      screenshot("favorites-" + width);
      page.getByRole(
              AriaRole.BUTTON,
              new Page.GetByRoleOptions().setName("Bỏ yêu thích Những ngày xanh").setExact(true))
          .focus();
      page.keyboard().press("Enter");
      page.waitForURL("**/favorites");
      assertThat(page.locator("article.book")).hasCount(0);
      assertThat(page.locator("main")).containsText("Chưa có sách yêu thích");
      noOverflow();
    }
  }

  @Test
  void sameAccountSharesFavoritesButDifferentAccountCannotSeeThem() {
    String email = account();
    String different = account();
    loginCustomer(page, email);
    save(page);
    page.navigate(url("/favorites"));
    try (var secondContext = browser.newContext()) {
      Page second = secondContext.newPage();
      loginCustomer(second, email);
      second.navigate(url("/favorites"));
      assertThat(second.locator("article.book")).hasCount(1);
      second.locator("main button").click();
      page.reload();
      assertThat(page.locator("article.book")).hasCount(0);
      save(page);
      page.navigate(url("/logout"));
      page.locator("main button").click();
      page.waitForURL("**/books?loggedOut");
      page.goBack();
      page.navigate(url("/favorites"));
      page.waitForURL("**/login?next=favorites");
      loginCustomer(page, email, "/favorites");
      page.navigate(url("/favorites"));
      assertThat(page.locator("article.book")).hasCount(1);
      second.navigate(url("/logout"));
      second.locator("main button").click();
      loginCustomer(second, different);
      second.navigate(url("/favorites"));
      assertThat(second.locator("article.book")).hasCount(0);
      page.reload();
      assertThat(page.locator("article.book")).hasCount(1);
    }
  }

  @Test
  void anonymousEntryReturnsToEmptyListWithoutSavingAnything() {
    String email = account();
    page.navigate(url("/books/" + Fixtures.BOOK));
    page.locator("main a[href='/login?next=favorites']").click();
    assertThat(page).hasURL(url("/login?next=favorites"));
    page.locator("[name=email]").fill(email);
    page.locator("[name=password]").fill(CustomerFixtures.PASSWORD);
    page.locator("button").click();
    page.waitForURL("**/favorites");
    assertThat(page.locator("article.book")).hasCount(0);
  }
}

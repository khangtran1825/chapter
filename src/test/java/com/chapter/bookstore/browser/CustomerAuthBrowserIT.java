package com.chapter.bookstore.browser;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.*;

import com.chapter.bookstore.integration.CustomerFixtures;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CustomerAuthBrowserIT extends BrowserTestSupport {
  @org.springframework.beans.factory.annotation.Autowired
  org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext
      serverContext;

  private String prepareShoppingState(Page p, String name, int quantity) {
    p.navigate(url("/books/" + com.chapter.bookstore.integration.Fixtures.BOOK));
    p.locator("form[action='/cart/items'] button").click();
    p.navigate(url("/checkout"));
    fillDelivery(p, name);
    p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Xác nhận đặt sách")).click();
    p.waitForURL("**/confirmation");
    String receipt = p.url();
    p.navigate(url("/books/" + com.chapter.bookstore.integration.Fixtures.BOOK));
    p.locator("[name=quantity]").fill(Integer.toString(quantity));
    p.locator("form[action='/cart/items'] button").click();
    p.navigate(url("/checkout"));
    fillDelivery(p, name);
    p.locator("[name=phone]").fill("123");
    p.locator("form.split").evaluate("f=>f.noValidate=true");
    p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Xác nhận đặt sách")).click();
    assertThat(p.locator("[name=recipientName]")).hasValue(name);
    return receipt;
  }

  private void fillDelivery(Page p, String name) {
    p.getByLabel("Họ và tên").fill(name);
    p.getByLabel("Số điện thoại").fill("0901234567");
    p.getByLabel("Số nhà, tên đường").fill("12 Đường Sách");
    p.getByLabel("Phường / xã").fill("Phường Mẫu");
    p.getByLabel("Quận / huyện").fill("Quận Mẫu");
    p.getByLabel("Tỉnh / thành phố").fill("Hà Nội");
  }

  @Test
  void guestRegistrationReturnsToCheckoutAndOldIdCannotClaimReceipt() {
    jdbc.update(
        "update book set stock=20 where id=?", com.chapter.bookstore.integration.Fixtures.BOOK);
    String receipt = prepareShoppingState(page, "Khách vãng lai", 1);
    String oldId =
        context.cookies().stream()
            .filter(c -> c.name.equals("JSESSIONID"))
            .findFirst()
            .orElseThrow()
            .value;
    long orders = jdbc.queryForObject("select count(*) from customer_order", Long.class);
    page.locator("a[href='/register?next=checkout']").click();
    String email = "continue-" + UUID.randomUUID() + "@example.com";
    register(page, email, CustomerFixtures.PASSWORD);
    page.locator("[name=email]").fill(email);
    page.locator("[name=password]").fill(CustomerFixtures.PASSWORD);
    page.locator("button").click();
    page.waitForURL("**/checkout");
    assertThat(page.locator("[name=recipientName]")).hasValue("Khách vãng lai");
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject("select count(*) from customer_order", Long.class))
        .isEqualTo(orders);
    org.assertj.core.api.Assertions.assertThat(page.navigate(receipt).status()).isEqualTo(200);
    try (var old = browser.newContext()) {
      old.addCookies(
          List.of(
              new com.microsoft.playwright.options.Cookie("JSESSIONID", oldId).setUrl(url("/"))));
      var stale = old.newPage();
      stale.navigate(url("/account"));
      assertThat(stale).hasURL(url("/login?next=account&expired"));
      org.assertj.core.api.Assertions.assertThat(stale.navigate(receipt).status()).isEqualTo(404);
    }
  }

  @Test
  void containerExpiresOnlyIdleSessionAndRejectsOldCookie() throws Exception {
    String email = "expiry-" + UUID.randomUUID() + "@example.com";
    register(page, email, CustomerFixtures.PASSWORD);
    try (var b = browser.newContext()) {
      Page second = b.newPage();
      customerLogin(page, email, CustomerFixtures.PASSWORD);
      customerLogin(second, email, CustomerFixtures.PASSWORD);
      String id =
          context.cookies().stream()
              .filter(c -> c.name.equals("JSESSIONID"))
              .findFirst()
              .orElseThrow()
              .value;
      var tomcat =
          ((org.springframework.boot.tomcat.TomcatWebServer) serverContext.getWebServer())
              .getTomcat();
      org.apache.catalina.Context app =
          (org.apache.catalina.Context) tomcat.getHost().findChildren()[0];
      var session = (org.apache.catalina.session.StandardSession) app.getManager().findSession(id);
      org.assertj.core.api.Assertions.assertThat(session.getMaxInactiveInterval()).isEqualTo(1800);
      session.setCreationTime(
          System.currentTimeMillis() - java.time.Duration.ofMinutes(31).toMillis());
      org.assertj.core.api.Assertions.assertThat(session.isValid()).isFalse();
      page.navigate(url("/account"));
      page.waitForURL("**/login?next=account&expired");
      second.navigate(url("/account"));
      assertThat(second.locator("main")).containsText(email);
    }
  }

  @Test
  void receiptDraftCartAndCurrentLogoutStayLocal() {
    jdbc.update(
        "update book set stock=20 where id=?", com.chapter.bookstore.integration.Fixtures.BOOK);
    String email = "owners-" + UUID.randomUUID() + "@example.com";
    register(page, email, CustomerFixtures.PASSWORD);
    try (var b = browser.newContext()) {
      Page second = b.newPage();
      customerLogin(page, email, CustomerFixtures.PASSWORD);
      String receiptA = prepareShoppingState(page, "Người đọc A", 1);
      customerLogin(second, email, CustomerFixtures.PASSWORD);
      String receiptB = prepareShoppingState(second, "Người đọc B", 2);
      page.navigate(url("/checkout"));
      assertThat(page.locator("[name=recipientName]")).hasValue("Người đọc A");
      org.assertj.core.api.Assertions.assertThat(page.navigate(receiptB).status()).isEqualTo(404);
      org.assertj.core.api.Assertions.assertThat(second.navigate(receiptA).status()).isEqualTo(404);
      page.navigate(url("/logout"));
      page.locator("main button").click();
      second.navigate(url("/checkout"));
      assertThat(second.locator("[name=recipientName]")).hasValue("Người đọc B");
      second.navigate(url("/cart"));
      assertThat(second.locator("[name=quantity]")).hasValue("2");
      org.assertj.core.api.Assertions.assertThat(second.navigate(receiptB).status()).isEqualTo(200);
      org.assertj.core.api.Assertions.assertThat(page.navigate(receiptA).status()).isEqualTo(404);
    }
  }

  private void register(Page p, String email, String password) {
    p.navigate(url("/register"));
    p.getByLabel("Họ và tên", new Page.GetByLabelOptions().setExact(true))
        .fill("Bạn đọc thử nghiệm");
    p.getByLabel("Email", new Page.GetByLabelOptions().setExact(true)).fill(email);
    p.getByLabel("Mật khẩu", new Page.GetByLabelOptions().setExact(true)).fill(password);
    p.locator("[name=confirmPassword]").fill(password);
    p.locator("button").click();
    p.waitForURL("**/login?registered");
  }

  private void customerLogin(Page p, String email, String password) {
    p.navigate(url("/login"));
    p.locator("[name=email]").fill(email);
    p.locator("[name=password]").fill(password);
    p.locator("button").click();
    p.waitForURL("**/books");
  }

  @Test
  void registrationLoginAndResponsiveAccount() {
    String email = "browser-" + UUID.randomUUID() + "@example.com";
    String password = "😀".repeat(64);
    register(page, email, password);
    assertThat(page.locator("[name=password]")).hasValue("");
    customerLogin(page, email, password);
    page.navigate(url("/account"));
    assertThat(page.locator("main")).containsText(email);
    for (int width : new int[] {360, 768, 1440}) {
      page.setViewportSize(width, 1000);
      noOverflow();
      screenshot("customer-account-" + width);
    }
    page.navigate(url("/register"));
    assertThat(page).hasURL(url("/account"));
    page.navigate(url("/logout"));
    assertThat(page.locator("main")).containsText("Các thiết bị khác");
    page.locator("main button").click();
    page.waitForURL("**/books?loggedOut");
    page.goBack();
    page.navigate(url("/account"));
    page.waitForURL("**/login?next=account");
    assertThat(page.locator("main")).not().containsText(email);
  }

  @Test
  void errorFormsNeverReturnSecretsAndSupportKeyboard() {
    String secret = "fictional-wrong-password-123";
    for (int width : new int[] {360, 768, 1440}) {
      page.setViewportSize(width, 1000);
      page.navigate(url("/register"));
      page.locator("[name=fullName]").fill("Bạn đọc");
      page.locator("[name=email]").fill("invalid-" + UUID.randomUUID() + "@example.com");
      page.locator("[name=password]").fill(secret);
      page.locator("[name=confirmPassword]").fill("different-password-123");
      page.locator("button").click();
      assertThat(page.locator("[name=password]")).hasValue("");
      assertThat(page.locator("[name=confirmPassword]")).hasValue("");
      org.assertj.core.api.Assertions.assertThat(page.content()).doesNotContain(secret);
      noOverflow();
      screenshot("customer-register-error-" + width);
      page.keyboard().press("Tab");
      org.assertj.core.api.Assertions.assertThat(
              page.evaluate("() => document.activeElement !== document.body"))
          .isEqualTo(true);
    }
    page.navigate(url("/login"));
    page.locator("[name=email]").fill("missing-" + UUID.randomUUID() + "@example.com");
    page.locator("[name=password]").fill(secret);
    page.locator("button").click();
    page.waitForURL("**/login?error");
    assertThat(page.locator("[name=password]")).hasValue("");
    assertThat(page.locator("[role=alert]")).isVisible();
    page.locator("[name=password]").fill("x");
    page.locator("button").click();
    assertThat(page.locator("[role=alert]")).containsText("12 đến 64");
    assertThat(page.locator("[name=password]")).hasValue("");
  }

  @Test
  void twoRealBrowserSessionsKeepIndependentCarts() {
    String email = "multi-" + UUID.randomUUID() + "@example.com";
    register(page, email, CustomerFixtures.PASSWORD);
    try (var b = browser.newContext()) {
      var second = b.newPage();
      customerLogin(page, email, CustomerFixtures.PASSWORD);
      page.navigate(url("/books/" + com.chapter.bookstore.integration.Fixtures.BOOK));
      page.locator("form[action='/cart/items'] button").click();
      customerLogin(second, email, CustomerFixtures.PASSWORD);
      second.navigate(url("/cart"));
      assertThat(second.locator("main")).containsText("Giỏ sách đang trống");
      page.navigate(url("/cart"));
      assertThat(page.locator(".cart-line")).hasCount(1);
      page.navigate(url("/logout"));
      page.locator("main button").click();
      second.navigate(url("/account"));
      assertThat(second.locator("main")).containsText(email);
      page.navigate(url("/account"));
      page.waitForURL("**/login?next=account");
    }
  }
}

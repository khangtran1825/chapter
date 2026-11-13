package com.chapter.bookstore.browser;

import static org.assertj.core.api.Assertions.*;

import com.chapter.bookstore.integration.*;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

@Tag("browser")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "server.address=127.0.0.1",
      "server.servlet.session.cookie.secure=false",
      "CHAPTER_ADMIN_USERNAME=browseradmin",
      "CHAPTER_ADMIN_PASSWORD=fictional-browser-test-123"
    })
public abstract class BrowserTestSupport extends PostgresTestSupport {
  @LocalServerPort protected int port;
  @Autowired protected JdbcTemplate jdbc;
  protected Playwright playwright;
  protected Browser browser;
  protected BrowserContext context;
  protected Page page;

  @RegisterExtension
  final AfterTestExecutionCallback evidence =
      execution -> {
        if (execution.getExecutionException().isPresent() && page != null && !page.isClosed()) {
          Files.createDirectories(Path.of("target/screenshots"));
          page.screenshot(
              new Page.ScreenshotOptions()
                  .setPath(
                      Path.of("target/screenshots/" + getClass().getSimpleName() + "-failure.png"))
                  .setFullPage(true));
        }
      };

  protected String url(String path) {
    return "http://127.0.0.1:" + port + path;
  }

  @BeforeAll
  void startBrowser() {
    playwright = Playwright.create();
    browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
  }

  @BeforeEach
  void fresh() {
    Fixtures.reset(jdbc);
    for (int i = 0; i < 15; i++) {
      UUID id =
          Fixtures.book(
              jdbc,
              UUID.randomUUID(),
              "Một câu chuyện bình yên " + i,
              "Tác giả mẫu",
              Fixtures.OTHER,
              10,
              true);
      jdbc.update(
          "update book set cover_path=? where id=?",
          "/images/cover-" + new String[] {"green", "orange", "blue", "cream"}[i % 4] + ".svg",
          id);
    }
    jdbc.update("update book set cover_path='/images/cover-green.svg' where id=?", Fixtures.BOOK);
    context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 1000));
    page = context.newPage();
    page.setDefaultTimeout(10000);
  }

  @AfterEach
  void closeContext() {
    if (context != null) context.close();
  }

  @AfterAll
  void stopBrowser() {
    if (browser != null) browser.close();
    if (playwright != null) playwright.close();
  }

  protected void noOverflow() {
    assertThat(
            ((Number)
                    page.evaluate("() => document.documentElement.scrollWidth - window.innerWidth"))
                .intValue())
        .isLessThanOrEqualTo(1);
  }

  protected void screenshot(String name) {
    page.screenshot(
        new Page.ScreenshotOptions()
            .setPath(Path.of("target/screenshots/" + name + ".png"))
            .setFullPage(true));
  }

  protected void login() {
    page.navigate(url("/admin/login"));
    page.getByLabel("Tên đăng nhập").fill("browseradmin");
    page.getByLabel("Mật khẩu").fill("fictional-browser-test-123");
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Đăng nhập").setExact(true))
        .click();
    page.waitForURL("**/admin/books");
  }

  protected void delivery() {
    page.getByLabel("Họ và tên").fill("Người đọc thử nghiệm");
    page.getByLabel("Số điện thoại").fill("0901234567");
    page.getByLabel("Số nhà, tên đường").fill("12 Đường Sách");
    page.getByLabel("Phường / xã").fill("Phường Mẫu");
    page.getByLabel("Quận / huyện").fill("Quận Mẫu");
    page.getByLabel("Tỉnh / thành phố").fill("Hà Nội");
  }
}

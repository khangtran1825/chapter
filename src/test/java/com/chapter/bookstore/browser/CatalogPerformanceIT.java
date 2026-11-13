package com.chapter.bookstore.browser;

import static org.assertj.core.api.Assertions.*;

import com.chapter.bookstore.integration.Fixtures;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;

@Tag("performance")
class CatalogPerformanceIT extends BrowserTestSupport {
  @Test
  void measuresTwentyConcurrentIsolatedShoppersWithOneThousandBooks() throws Exception {
    jdbc.execute("truncate book cascade");
    jdbc.execute(
        "insert into"
            + " book(id,title,author,description,price_vnd,stock,active,version,created_at,updated_at,category_id)"
            + " select md5(n::text)::uuid,'Performance Book '||lpad(n::text,4,'0'),'Sample"
            + " Author','Fictional load-test data',125000,10,true,0,now(),now(),'"
            + Fixtures.CAT
            + "'::uuid from generate_series(1,1000) n");
    List<BrowserContext> shoppers = new ArrayList<>();
    List<Page> pages = new ArrayList<>();
    try {
      for (int i = 0; i < 20; i++) {
        var c = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 900));
        shoppers.add(c);
        var p = c.newPage();
        p.navigate(url("/books"));
        p.waitForLoadState(LoadState.NETWORKIDLE);
        pages.add(p);
      }
      long started = System.nanoTime();
      for (var p : pages)
        p.evaluate("() => { setTimeout(() => { location.href='/books?q=Performance'; }, 3000); }");
      List<Double> times = new ArrayList<>();
      for (var p : pages) {
        p.waitForURL("**/books?q=Performance");
        p.locator(".book").first().waitFor();
        p.waitForLoadState(LoadState.NETWORKIDLE);
        times.add(
            ((Number)
                    p.evaluate(
                        "() => {const n=performance.getEntriesByType('navigation')[0]; const"
                            + " f=performance.getEntriesByName('first-contentful-paint')[0]; return"
                            + " Math.max(n.domContentLoadedEventEnd, f?f.startTime:0);}"))
                .doubleValue());
        assertThat(p.locator(".book").count()).isEqualTo(12);
      }
      var sorted = new ArrayList<>(times);
      Collections.sort(sorted);
      double p95 = sorted.get(18);
      Files.createDirectories(Path.of("target/performance"));
      String result =
          "{\"books\":1000,\"sessions\":20,\"p95Ms\":"
              + p95
              + ",\"samplesMs\":"
              + times
              + ",\"elapsedSeconds\":"
              + (System.nanoTime() - started) / 1e9
              + ",\"browser\":\""
              + browser.version()
              + "\",\"os\":\""
              + System.getProperty("os.name")
              + "\",\"processors\":"
              + Runtime.getRuntime().availableProcessors()
              + ",\"warmup\":\"one catalog load per"
              + " session\",\"network\":\"loopback\",\"postgres\":\"18.7\"}";
      Files.writeString(Path.of("target/performance/catalog.json"), result);
      System.out.println("CATALOG_PERFORMANCE " + result);
      assertThat(p95).isLessThanOrEqualTo(2000);
    } finally {
      for (var c : shoppers) c.close();
    }
  }
}

package com.chapter.bookstore.integration;

import static org.assertj.core.api.Assertions.*;

import com.chapter.bookstore.catalog.CatalogService;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class CatalogQueryIT extends PostgresTestSupport {
  @Autowired CatalogService catalog;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void seed() {
    Fixtures.reset(jdbc);
  }

  @Test
  void searchIsTrimmedLiteralAndCategoryCombined() {
    assertThat(catalog.search("  an NHIÊN  ", Fixtures.CAT, 0).getTotalElements()).isEqualTo(1);
    assertThat(catalog.search("ngay", null, 0).getTotalElements()).isZero();
    assertThat(catalog.search("%", null, 0).getTotalElements()).isZero();
    assertThat(catalog.search("_", null, 0).getTotalElements()).isZero();
    assertThat(catalog.search("ngày", Fixtures.OTHER, 0).getTotalElements()).isZero();
  }

  @Test
  void excludesArchivedButKeepsStockZeroAndStablePagination() {
    Fixtures.book(jdbc, UUID.randomUUID(), "Archived", "A", Fixtures.CAT, 3, false);
    for (int i = 0; i < 14; i++)
      Fixtures.book(jdbc, UUID.randomUUID(), "Book " + i, "A", Fixtures.CAT, 0, true);
    var first = catalog.search("", null, 0);
    var second = catalog.search("", null, 1);
    assertThat(first.getTotalElements()).isEqualTo(15);
    assertThat(first.getContent()).hasSize(12);
    assertThat(second.getContent()).hasSize(3);
    assertThat(first.getContent()).doesNotContainAnyElementsOf(second.getContent());
  }
}

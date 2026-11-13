package com.chapter.bookstore.integration;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;

public final class Fixtures {
  public static final UUID CAT = UUID.fromString("10000000-0000-0000-0000-000000000001");
  public static final UUID OTHER = UUID.fromString("10000000-0000-0000-0000-000000000002");
  public static final UUID BOOK = UUID.fromString("20000000-0000-0000-0000-000000000001");

  public static void reset(JdbcTemplate jdbc) {
    if (jdbc.queryForObject("select to_regclass('customer_order') is not null", Boolean.class))
      jdbc.execute("truncate customer_order,book,category cascade");
    else jdbc.execute("truncate book,category cascade");
    jdbc.update(
        "insert into category values (?, 'Văn học', now()), (?, 'Khám phá', now())", CAT, OTHER);
    book(jdbc, BOOK, "Những ngày xanh", "An Nhiên", CAT, 3, true);
  }

  public static UUID book(
      JdbcTemplate j, UUID id, String title, String author, UUID cat, int stock, boolean active) {
    j.update(
        "insert into"
            + " book(id,title,author,description,price_vnd,stock,active,version,created_at,updated_at,category_id)"
            + " values (?,?,?,'Mô tả mẫu',125000,?,?,0,now(),now(),?)",
        id,
        title,
        author,
        stock,
        active,
        cat);
    return id;
  }
}

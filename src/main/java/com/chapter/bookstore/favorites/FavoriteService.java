package com.chapter.bookstore.favorites;

import com.chapter.bookstore.shared.WebProblem;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FavoriteService {
  private final JdbcTemplate jdbc;

  public FavoriteService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional
  public void add(UUID customer, UUID book) {
    // The share lock keeps an admin hide/delete from racing the visibility check.
    boolean available =
        Boolean.TRUE.equals(
            jdbc.queryForObject(
                "with available as (select id from book where id=? and active for share), saved as"
                    + " (insert into customer_favorite(customer_id,book_id) select ?,id from"
                    + " available on conflict do nothing) select exists(select 1 from available)",
                Boolean.class,
                book,
                customer));
    if (!available) throw new WebProblem(404, "Không tìm thấy sách đang bày bán.");
  }

  public Set<UUID> savedIds(UUID customer, Collection<UUID> books) {
    if (books.isEmpty()) return Set.of();
    var args = new ArrayList<Object>();
    args.add(customer);
    args.addAll(books);
    String placeholders = String.join(",", Collections.nCopies(books.size(), "?"));
    return new HashSet<>(
        jdbc.query(
            "select book_id from customer_favorite where customer_id=? and book_id in ("
                + placeholders
                + ")",
            (r, i) -> r.getObject(1, UUID.class),
            args.toArray()));
  }

  public record FavoriteBook(
      UUID id, String title, String author, String coverPath, long priceVnd, int stock) {
    public String getCover() {
      return com.chapter.bookstore.shared.Presentation.resolveCover(coverPath);
    }
  }

  public org.springframework.data.domain.Page<FavoriteBook> list(UUID customer, int page) {
    if (page < 0 || page > 100000) throw new WebProblem(400, "Số trang không hợp lệ.");
    long total =
        jdbc.queryForObject(
            "select count(*) from customer_favorite f join book b on b.id=f.book_id where"
                + " f.customer_id=? and b.active",
            Long.class,
            customer);
    var rows =
        jdbc.query(
            "select b.id,b.title,b.author,b.cover_path,b.price_vnd,b.stock from customer_favorite f"
                + " join book b on b.id=f.book_id where f.customer_id=? and b.active order by"
                + " f.created_at desc,f.book_id asc limit 12 offset ?",
            (r, i) ->
                new FavoriteBook(
                    r.getObject("id", UUID.class),
                    r.getString("title"),
                    r.getString("author"),
                    r.getString("cover_path"),
                    r.getLong("price_vnd"),
                    r.getInt("stock")),
            customer,
            (long) page * 12);
    return new org.springframework.data.domain.PageImpl<>(
        rows, org.springframework.data.domain.PageRequest.of(page, 12), total);
  }

  @Transactional
  public void remove(UUID customer, UUID book) {
    jdbc.update("delete from customer_favorite where customer_id=? and book_id=?", customer, book);
  }
}

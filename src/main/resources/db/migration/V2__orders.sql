CREATE TABLE customer_order (
 id uuid PRIMARY KEY, reference varchar(40) NOT NULL UNIQUE, checkout_key uuid NOT NULL UNIQUE,
 request_fingerprint varchar(64) NOT NULL, owner_token_hash varchar(64) NOT NULL,
 recipient_name varchar(100) NOT NULL CHECK(length(trim(recipient_name))>0),
 phone varchar(10) NOT NULL CHECK(phone ~ '^0[0-9]{9}$'), street varchar(255) NOT NULL CHECK(length(trim(street))>0),
 ward varchar(100) NOT NULL CHECK(length(trim(ward))>0), district varchar(100) NOT NULL CHECK(length(trim(district))>0), province varchar(100) NOT NULL CHECK(length(trim(province))>0),
 subtotal_vnd bigint NOT NULL CHECK(subtotal_vnd>=0), delivery_vnd bigint NOT NULL CHECK(delivery_vnd=0),
 total_vnd bigint NOT NULL CHECK(total_vnd=subtotal_vnd+delivery_vnd),
 status varchar(20) NOT NULL CHECK(status IN ('PENDING','CONFIRMED','SHIPPING','COMPLETED','CANCELLED')),
 created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, version bigint NOT NULL DEFAULT 0
);
CREATE TABLE order_line (
 id uuid PRIMARY KEY, order_id uuid NOT NULL REFERENCES customer_order(id), book_id uuid NOT NULL REFERENCES book(id),
 title_snapshot varchar(255) NOT NULL, author_snapshot varchar(255) NOT NULL,
 unit_price_vnd bigint NOT NULL CHECK(unit_price_vnd>=0), quantity integer NOT NULL CHECK(quantity>0),
 line_total_vnd bigint NOT NULL CHECK(line_total_vnd=unit_price_vnd*quantity)
);
CREATE INDEX order_recent ON customer_order(created_at DESC,id);
CREATE INDEX order_line_order ON order_line(order_id);

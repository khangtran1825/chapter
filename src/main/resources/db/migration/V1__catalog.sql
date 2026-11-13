CREATE TABLE category (
 id uuid PRIMARY KEY, name varchar(100) NOT NULL CHECK (length(trim(name)) > 0), created_at timestamptz NOT NULL
);
CREATE UNIQUE INDEX category_name_ci ON category(lower(name));
CREATE TABLE book (
 id uuid PRIMARY KEY, title varchar(255) NOT NULL CHECK (length(trim(title)) > 0),
 author varchar(255) NOT NULL CHECK (length(trim(author)) > 0), description varchar(10000) NOT NULL CHECK (length(trim(description)) > 0),
 cover_path varchar(255), price_vnd bigint NOT NULL CHECK(price_vnd >= 0), stock integer NOT NULL CHECK(stock >= 0),
 active boolean NOT NULL DEFAULT true, version bigint NOT NULL DEFAULT 0,
 created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
 category_id uuid NOT NULL REFERENCES category(id)
);
CREATE INDEX book_catalog ON book(active, title, id);
CREATE INDEX book_category ON book(category_id);

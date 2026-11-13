CREATE TABLE customer_favorite (
    customer_id uuid NOT NULL REFERENCES customer_account(id) ON DELETE CASCADE,
    book_id uuid NOT NULL REFERENCES book(id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (customer_id, book_id)
);
CREATE INDEX customer_favorite_newest ON customer_favorite (customer_id, created_at DESC, book_id ASC);

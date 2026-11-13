CREATE TABLE customer_account (
  id uuid PRIMARY KEY,
  full_name varchar(100) NOT NULL CHECK (length(btrim(full_name)) BETWEEN 1 AND 100),
  email_normalized varchar(254) NOT NULL UNIQUE CHECK (email_normalized = lower(btrim(email_normalized)) AND length(email_normalized) > 0),
  password_hash varchar(255) NOT NULL,
  enabled boolean NOT NULL DEFAULT true,
  role varchar(20) NOT NULL DEFAULT 'CUSTOMER' CHECK (role = 'CUSTOMER'),
  created_at timestamptz NOT NULL
);

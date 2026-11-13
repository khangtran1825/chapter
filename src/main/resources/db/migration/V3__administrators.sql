CREATE TABLE admin_user (
 id uuid PRIMARY KEY, username varchar(100) NOT NULL UNIQUE CHECK(username=lower(username) AND length(username)>=3),
 password_hash varchar(255) NOT NULL, enabled boolean NOT NULL, role varchar(20) NOT NULL CHECK(role='ADMIN')
);

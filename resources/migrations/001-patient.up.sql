CREATE TYPE gender AS ENUM ('male', 'female');

CREATE table patient (
  id SERIAL PRIMARY KEY,
  full_name VARCHAR NOT NULL,
  gender gender NOT NULL,
  birthday date NOT NULL,
  address VARCHAR,
  insurance CHAR(16) NOT NULL);

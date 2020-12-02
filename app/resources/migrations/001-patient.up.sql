CREATE TYPE gender AS ENUM (
  'male', 'female'
);

CREATE table patient (
  id SERIAL PRIMARY KEY,
  full_name VARCHAR,
  gender gender,
  birthday date,
  address VARCHAR,
  insurance int);

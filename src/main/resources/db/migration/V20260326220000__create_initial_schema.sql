CREATE TABLE addresses (
    id BIGSERIAL PRIMARY KEY,
    street_name VARCHAR(255),
    street_number VARCHAR(255),
    complement VARCHAR(255),
    city VARCHAR(255),
    state VARCHAR(255),
    zip_code VARCHAR(255)
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    cpf VARCHAR(255),
    genre VARCHAR(255),
    birth_date DATE,
    email VARCHAR(255),
    password VARCHAR(255),
    profile VARCHAR(255),
    address_id BIGINT,
    CONSTRAINT fk_users_address FOREIGN KEY (address_id) REFERENCES addresses(id)
);

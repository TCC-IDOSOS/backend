CREATE TABLE health_unit (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    cnpj VARCHAR(255),
    phone VARCHAR(255),
    email VARCHAR(255),
    address_id BIGINT,
    CONSTRAINT fk_health_unit_address FOREIGN KEY (address_id) REFERENCES addresses(id)
);
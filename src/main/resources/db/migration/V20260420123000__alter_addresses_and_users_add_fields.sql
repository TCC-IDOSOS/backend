ALTER TABLE addresses
ADD COLUMN neighborhood VARCHAR(255);

ALTER TABLE users
ADD COLUMN health_unit_id BIGINT;

ALTER TABLE users
ADD CONSTRAINT fk_users_health_unit
FOREIGN KEY (health_unit_id) REFERENCES health_unit(id);
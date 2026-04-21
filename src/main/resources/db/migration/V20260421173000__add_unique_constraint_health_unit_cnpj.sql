ALTER TABLE health_unit
ADD CONSTRAINT uq_health_unit_cnpj UNIQUE (cnpj);
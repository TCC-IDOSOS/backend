ALTER TABLE test_execution
ADD COLUMN health_unit_id BIGINT;

ALTER TABLE test_execution
ADD CONSTRAINT fk_test_execution_health_unit
FOREIGN KEY (health_unit_id)
REFERENCES health_unit(id);
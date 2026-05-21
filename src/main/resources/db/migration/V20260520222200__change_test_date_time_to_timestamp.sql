ALTER TABLE test_result
ALTER COLUMN test_date_time TYPE TIMESTAMP
USING test_date_time::timestamp;
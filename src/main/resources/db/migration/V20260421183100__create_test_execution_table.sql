CREATE TABLE test_execution (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    test_type VARCHAR(50) NOT NULL,
    test_date_time VARCHAR(50) NOT NULL,
    sensor VARCHAR(100),
    frequency INTEGER,
    total_repetitions_app INTEGER,
    raw_s3_key VARCHAR(500),
    processed_s3_key VARCHAR(500),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_test_execution_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);
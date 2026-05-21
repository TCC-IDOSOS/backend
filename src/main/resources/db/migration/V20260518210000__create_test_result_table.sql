CREATE TABLE test_result (
    id BIGSERIAL PRIMARY KEY,

    test_execution_id BIGINT NOT NULL,

    patient_name VARCHAR(255) NOT NULL,
    patient_age INTEGER NOT NULL,
    patient_sex VARCHAR(50) NOT NULL,

    test_type VARCHAR(50) NOT NULL,
    test_date_time VARCHAR(50) NOT NULL,

    health_unit_id BIGINT NOT NULL,

    total_repetitions INTEGER,
    average_height FLOAT,
    cadence FLOAT,
    test_classification VARCHAR(100),

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,

    CONSTRAINT fk_test_result_test_execution
        FOREIGN KEY (test_execution_id) REFERENCES test_execution(id),

    CONSTRAINT fk_test_result_health_unit
        FOREIGN KEY (health_unit_id) REFERENCES health_unit(id),

    CONSTRAINT uk_test_result_test_execution
        UNIQUE (test_execution_id)
);
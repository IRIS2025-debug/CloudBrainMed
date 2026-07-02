CREATE TABLE IF NOT EXISTS ai_training_task (
    task_id VARCHAR(64) PRIMARY KEY,
    model_key VARCHAR(128) NOT NULL,
    model_type VARCHAR(64) NOT NULL,
    hyper_params TEXT,
    dataset_path TEXT,
    status VARCHAR(32) NOT NULL,
    model_id VARCHAR(64),
    error_message TEXT,
    create_time TIMESTAMP,
    start_time TIMESTAMP,
    completed_time TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_training_task_create_time
    ON ai_training_task (create_time DESC);

CREATE INDEX IF NOT EXISTS idx_ai_training_task_status
    ON ai_training_task (status);

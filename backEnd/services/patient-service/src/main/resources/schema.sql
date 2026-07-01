CREATE TABLE IF NOT EXISTS register_report (
    record_id VARCHAR(32) PRIMARY KEY,
    patient_id VARCHAR(32),
    doctor_id VARCHAR(32),
    register_id VARCHAR(32),
    doctor_name VARCHAR(20),
    patient_name VARCHAR(20),
    visit_age INTEGER,
    description TEXT,
    visit_date DATE,
    pay_status VARCHAR(20),
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE register_report
    ADD COLUMN IF NOT EXISTS record_id VARCHAR(32),
    ADD COLUMN IF NOT EXISTS patient_id VARCHAR(32),
    ADD COLUMN IF NOT EXISTS doctor_id VARCHAR(32),
    ADD COLUMN IF NOT EXISTS register_id VARCHAR(32),
    ADD COLUMN IF NOT EXISTS doctor_name VARCHAR(20),
    ADD COLUMN IF NOT EXISTS patient_name VARCHAR(20),
    ADD COLUMN IF NOT EXISTS visit_age INTEGER,
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS visit_date DATE,
    ADD COLUMN IF NOT EXISTS pay_status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_register_report_register_id
    ON register_report(register_id);

CREATE INDEX IF NOT EXISTS idx_register_report_patient_id_visit_date
    ON register_report(patient_id, visit_date DESC);

ALTER TABLE prescription
    ADD COLUMN IF NOT EXISTS medicine_id VARCHAR(32);

ALTER TABLE patient
    ADD COLUMN IF NOT EXISTS avatar VARCHAR(255);

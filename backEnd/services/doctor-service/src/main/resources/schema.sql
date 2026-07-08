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

ALTER TABLE medical_order
    ADD COLUMN IF NOT EXISTS assigned_room VARCHAR(100);

CREATE TABLE IF NOT EXISTS medical_report (
    report_id VARCHAR(32) PRIMARY KEY,
    order_item_id VARCHAR(32) NOT NULL UNIQUE,
    patient_id VARCHAR(32) NOT NULL,
    item_category VARCHAR(20) NOT NULL,
    result_summary TEXT,
    conclusion TEXT,
    abnormal_flag VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    attachment_url VARCHAR(255),
    ai_result_json TEXT,
    report_doctor_id VARCHAR(32),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    performed_time TIMESTAMPTZ,
    report_time TIMESTAMPTZ,
    create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMPTZ
);

ALTER TABLE medical_report
    ADD COLUMN IF NOT EXISTS report_id VARCHAR(32),
    ADD COLUMN IF NOT EXISTS order_item_id VARCHAR(32),
    ADD COLUMN IF NOT EXISTS patient_id VARCHAR(32),
    ADD COLUMN IF NOT EXISTS item_category VARCHAR(20),
    ADD COLUMN IF NOT EXISTS result_summary TEXT,
    ADD COLUMN IF NOT EXISTS conclusion TEXT,
    ADD COLUMN IF NOT EXISTS abnormal_flag VARCHAR(20) DEFAULT 'NORMAL',
    ADD COLUMN IF NOT EXISTS attachment_url VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ai_result_json TEXT,
    ADD COLUMN IF NOT EXISTS report_doctor_id VARCHAR(32),
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'DRAFT',
    ADD COLUMN IF NOT EXISTS performed_time TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS report_time TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS update_time TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS uk_medical_report_order_item_id
    ON medical_report(order_item_id);

CREATE INDEX IF NOT EXISTS idx_medical_report_patient_id_report_time
    ON medical_report(patient_id, report_time DESC);

package com.cloudbrainmed.doctor;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaSqlTest {

    @Test
    void schemaCreatesMedicalReportTableForReportHandoff() throws Exception {
        String schema = Files.readString(Path.of("src/main/resources/schema.sql"));

        assertThat(schema).contains("CREATE TABLE IF NOT EXISTS medical_report");
        assertThat(schema).contains("UNIQUE");
        assertThat(schema).contains("order_item_id");
        assertThat(schema).contains("ai_result_json TEXT");
    }
}

package com.cloudbrainmed.doctor.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConsultRecordTest {

    @Test
    void exposesMedicalOrderAndReturnedReportFlagsFromCounts() {
        ConsultRecord record = new ConsultRecord();

        record.setMedicalOrderCount(0);
        record.setReportCount(0);
        assertThat(record.getHasMedicalOrder()).isFalse();
        assertThat(record.getHasReturnedReport()).isFalse();

        record.setMedicalOrderCount(2);
        record.setReportCount(1);
        assertThat(record.getHasMedicalOrder()).isTrue();
        assertThat(record.getHasReturnedReport()).isTrue();
    }
}

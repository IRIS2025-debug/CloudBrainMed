package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.doctor.entity.ConsultRecord;
import com.cloudbrainmed.doctor.mapper.ConsultMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultServiceImplTest {

    private final ConsultMapper mapper = mock(ConsultMapper.class);
    private final ConsultServiceImpl service = new ConsultServiceImpl(mapper);

    @Test
    void saveDraftRejectsAnotherDoctor() {
        ConsultRecord record = consult("D001", "PENDING");
        when(mapper.findDetail("R001")).thenReturn(record);

        assertThrows(BusinessException.class,
                () -> service.saveDraft("R001", "病历", "D002"));
    }

    @Test
    void saveDraftRejectsCompletedConsult() {
        ConsultRecord record = consult("D001", "COMPLETED");
        when(mapper.findDetail("R001")).thenReturn(record);

        assertThrows(BusinessException.class,
                () -> service.saveDraft("R001", "病历", "D001"));
    }

    @Test
    void completeRequiresConfirmedRecord() {
        ConsultRecord record = consult("D001", "IN_PROGRESS");
        when(mapper.findDetail("R001")).thenReturn(record);

        assertThrows(BusinessException.class,
                () -> service.completeConsult("R001", "D001"));
    }

    private ConsultRecord consult(String doctorId, String status) {
        ConsultRecord record = new ConsultRecord();
        record.setRegisterId("R001");
        record.setDoctorId(doctorId);
        record.setConsultStatus(status);
        return record;
    }
}

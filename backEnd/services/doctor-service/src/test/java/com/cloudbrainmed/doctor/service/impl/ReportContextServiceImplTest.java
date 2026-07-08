package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.doctor.entity.ConsultRecord;
import com.cloudbrainmed.doctor.mapper.ConsultMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportContextServiceImplTest {

    private final ConsultMapper consultMapper = mock(ConsultMapper.class);
    private final ReportContextServiceImpl service =
            new ReportContextServiceImpl(consultMapper);

    @Test
    void getContextRejectsExpiredVisitDate() {
        ConsultRecord detail = consult("D001");
        detail.setVisitDate(LocalDate.now().minusDays(1));
        when(consultMapper.findDetail("REG001")).thenReturn(detail);

        assertThrows(BusinessException.class,
                () -> service.getContext("REG001", "D001"));
    }

    @Test
    void getContextRejectsFutureVisitDate() {
        ConsultRecord detail = consult("D001");
        detail.setVisitDate(LocalDate.now().plusDays(1));
        when(consultMapper.findDetail("REG001")).thenReturn(detail);

        assertThrows(BusinessException.class,
                () -> service.getContext("REG001", "D001"));
    }

    private ConsultRecord consult(String doctorId) {
        ConsultRecord detail = new ConsultRecord();
        detail.setRegisterId("REG001");
        detail.setPatientId("P001");
        detail.setDoctorId(doctorId);
        detail.setChiefComplaint("headache");
        detail.setDescription("draft");
        detail.setConsultStatus("IN_PROGRESS");
        detail.setGender(1);
        detail.setPatientAge(32);
        when(consultMapper.findMedicalHistory("P001", "REG001"))
                .thenReturn(List.of());
        when(consultMapper.findPreviousReports("P001", "REG001"))
                .thenReturn(List.of());
        return detail;
    }
}

package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.doctor.dto.DoctorWorkRuleRequest;
import com.cloudbrainmed.doctor.entity.Doctor;
import com.cloudbrainmed.doctor.entity.DoctorWorkRule;
import com.cloudbrainmed.doctor.mapper.DoctorMapper;
import com.cloudbrainmed.doctor.mapper.DoctorWorkRuleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DoctorWorkRuleServiceImplTest {

    private DoctorMapper doctorMapper;
    private DoctorWorkRuleMapper workRuleMapper;
    private DoctorWorkRuleServiceImpl service;

    @BeforeEach
    void setUp() {
        doctorMapper = mock(DoctorMapper.class);
        workRuleMapper = mock(DoctorWorkRuleMapper.class);
        service = new DoctorWorkRuleServiceImpl(
                doctorMapper, workRuleMapper);

        Doctor doctor = new Doctor();
        doctor.setDoctorId("DOC001");
        doctor.setDepartmentId("DEPT001");
        doctor.setStatus(1);
        doctor.setIsDeleted(0);
        when(doctorMapper.selectById("DOC001")).thenReturn(doctor);
    }

    @Test
    void createKeepsGeneratedRuleId() {
        when(workRuleMapper.countOverlap(any())).thenReturn(0);

        DoctorWorkRule created =
                service.create(request(), "DOC001");

        ArgumentCaptor<DoctorWorkRule> captor =
                ArgumentCaptor.forClass(DoctorWorkRule.class);
        verify(workRuleMapper).insert(captor.capture());
        assertThat(created.getRuleId()).startsWith("DWR");
        assertThat(created.getRuleId()).hasSize(32);
        assertThat(captor.getValue().getRuleId())
                .isEqualTo(created.getRuleId());
    }

    @Test
    void rejectsOverlappingRule() {
        when(workRuleMapper.countOverlap(any())).thenReturn(1);

        assertThatThrownBy(() -> service.create(
                request(), "DOC001"))
                .isInstanceOf(BusinessException.class);
    }

    private DoctorWorkRuleRequest request() {
        DoctorWorkRuleRequest request = new DoctorWorkRuleRequest();
        request.setDayOfWeek(1);
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(12, 0));
        request.setMaxPatients(20);
        request.setPreferredLevel(4);
        request.setValidFrom(LocalDate.of(2026, 6, 15));
        request.setValidTo(LocalDate.of(2026, 12, 31));
        return request;
    }
}

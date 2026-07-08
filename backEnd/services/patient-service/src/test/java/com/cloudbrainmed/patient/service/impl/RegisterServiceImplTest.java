package com.cloudbrainmed.patient.service.impl;

import com.cloudbrainmed.patient.dto.RegisterSubmitDto;
import com.cloudbrainmed.patient.entity.Dept;
import com.cloudbrainmed.patient.entity.Doctor;
import com.cloudbrainmed.patient.entity.DoctorSchedule;
import com.cloudbrainmed.patient.entity.Patient;
import com.cloudbrainmed.patient.entity.Registration;
import com.cloudbrainmed.patient.mapper.DeptMapper;
import com.cloudbrainmed.patient.mapper.DoctorMapper;
import com.cloudbrainmed.patient.mapper.DoctorScheduleMapper;
import com.cloudbrainmed.patient.mapper.MedicalOrderItemMapper;
import com.cloudbrainmed.patient.mapper.MedicalOrderMapper;
import com.cloudbrainmed.patient.mapper.PatientMapper;
import com.cloudbrainmed.patient.mapper.PrescriptionMapper;
import com.cloudbrainmed.patient.mapper.RegisterReportMapper;
import com.cloudbrainmed.patient.mapper.RegistrationMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisterServiceImplTest {

    @Test
    void submitRegisterUsesScheduleDateAndTimeForRegistration() {
        PatientMapper patientMapper = mock(PatientMapper.class);
        DoctorMapper doctorMapper = mock(DoctorMapper.class);
        DeptMapper deptMapper = mock(DeptMapper.class);
        DoctorScheduleMapper doctorScheduleMapper = mock(DoctorScheduleMapper.class);
        RegistrationMapper registrationMapper = mock(RegistrationMapper.class);
        RegisterReportMapper registerReportMapper = mock(RegisterReportMapper.class);
        MedicalOrderMapper medicalOrderMapper = mock(MedicalOrderMapper.class);
        MedicalOrderItemMapper medicalOrderItemMapper = mock(MedicalOrderItemMapper.class);
        PrescriptionMapper prescriptionMapper = mock(PrescriptionMapper.class);
        RegisterServiceImpl service = new RegisterServiceImpl(
                deptMapper, doctorMapper, doctorScheduleMapper,
                patientMapper, registrationMapper, registerReportMapper,
                medicalOrderMapper, medicalOrderItemMapper, prescriptionMapper);

        Patient patient = new Patient();
        patient.setPatientId("P001");
        patient.setName("Alice");
        patient.setGender(2);
        patient.setBirthday(LocalDate.of(1990, 1, 1));
        patient.setIdCard("110101199001011234");
        patient.setPhone("13800000000");
        when(patientMapper.selectById("P001")).thenReturn(patient);

        Doctor doctor = new Doctor();
        doctor.setDoctorId("D001");
        doctor.setDepartmentId("DEPT001");
        when(doctorMapper.selectByDoctorId("D001")).thenReturn(doctor);

        Dept dept = new Dept();
        dept.setDeptId("DEPT001");
        dept.setDeptName("Neurology");
        when(deptMapper.selectById("DEPT001")).thenReturn(dept);

        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setScheduleId("S001");
        schedule.setDoctorId("D001");
        schedule.setWorkDate(LocalDate.of(2026, 7, 7));
        schedule.setStartTime(LocalTime.of(9, 0));
        schedule.setEndTime(LocalTime.of(9, 30));
        schedule.setRemainNum(3);
        schedule.setPrice(new BigDecimal("30.00"));
        schedule.setRoom("A101");
        when(doctorScheduleMapper.selectByScheduleId("S001")).thenReturn(schedule);
        when(doctorScheduleMapper.decrementRemainNum("S001")).thenReturn(1);

        RegisterSubmitDto dto = new RegisterSubmitDto();
        dto.setPatientId("P001");
        dto.setDoctorId("D001");
        dto.setScheduleId("S001");
        dto.setVisitDate(null);
        dto.setConsultTime("wrong-time");
        dto.setChiefComplaint("headache");

        Registration result = service.submitRegister(dto);

        ArgumentCaptor<Registration> captor =
                ArgumentCaptor.forClass(Registration.class);
        verify(registrationMapper).insert(captor.capture());
        Registration saved = captor.getValue();
        assertThat(saved.getVisitDate()).isEqualTo(LocalDate.of(2026, 7, 7));
        assertThat(saved.getConsultTime()).isEqualTo("09:00-09:30");
        assertThat(result.getVisitDate()).isEqualTo(saved.getVisitDate());
        assertThat(result.getConsultTime()).isEqualTo(saved.getConsultTime());
    }
}

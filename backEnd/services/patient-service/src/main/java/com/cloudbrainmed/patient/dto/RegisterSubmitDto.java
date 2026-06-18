// com.cloudbrainmed.patient.dto.RegisterSubmitDto.java
package com.cloudbrainmed.patient.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class RegisterSubmitDto {
    private String patientId;
    private String doctorId;
    private String scheduleId;
    private String chiefComplaint;
    private LocalDate visitDate;
    private String consultTime;
}
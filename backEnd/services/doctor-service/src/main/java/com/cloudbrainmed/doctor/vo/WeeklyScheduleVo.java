package com.cloudbrainmed.doctor.vo;

import com.cloudbrainmed.doctor.entity.DoctorSchedule;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class WeeklyScheduleVo {
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private List<WeekDayVo> weekData;

    @Data
    public static class WeekDayVo {
        private LocalDate date;
        private Integer dayOfWeek;
        private String dayName;
        private List<DoctorSchedule> schedules;
    }
}
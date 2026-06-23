package com.cloudbrainmed.doctor.service;

import com.cloudbrainmed.doctor.entity.DoctorSchedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DoctorScheduleService {

    /**
     * 获取医生某周的排班
     */
    List<DoctorSchedule> getWeeklySchedule(String doctorId, LocalDate weekStart);

    /**
     * 按日期分组获取排班
     */
    Map<LocalDate, List<DoctorSchedule>> getWeeklyScheduleGrouped(String doctorId, LocalDate weekStart);

    /**
     * 获取周的开始和结束日期（周一至周日）
     */
    LocalDate[] getWeekRange(LocalDate date);
}
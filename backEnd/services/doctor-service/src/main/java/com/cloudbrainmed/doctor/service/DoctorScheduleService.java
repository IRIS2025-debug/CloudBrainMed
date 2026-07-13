package com.cloudbrainmed.doctor.service;

import com.cloudbrainmed.doctor.entity.DoctorSchedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * 判断医生当前是否可接检查检验任务
     */
    boolean isDoctorAvailable(String doctorId, LocalDateTime now);

    /**
     * 批量过滤当前处于有效排班时间内的医生
     */
    Set<String> filterAvailableDoctors(List<String> doctorIds, LocalDateTime now);

    /**
     * 获取周的开始和结束日期（周一至周日）
     */
    LocalDate[] getWeekRange(LocalDate date);
}

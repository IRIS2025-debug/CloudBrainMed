package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.doctor.entity.DoctorSchedule;
import com.cloudbrainmed.doctor.mapper.DoctorScheduleMapper;
import com.cloudbrainmed.doctor.service.DoctorScheduleService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DoctorScheduleServiceImpl implements DoctorScheduleService {

    private final DoctorScheduleMapper scheduleMapper;

    public DoctorScheduleServiceImpl(DoctorScheduleMapper scheduleMapper) {
        this.scheduleMapper = scheduleMapper;
    }

    @Override
    public List<DoctorSchedule> getWeeklySchedule(String doctorId, LocalDate weekStart) {
        LocalDate[] range = getWeekRange(weekStart);
        return scheduleMapper.selectByDoctorIdAndWeek(doctorId, range[0], range[1]);
    }

    @Override
    public Map<LocalDate, List<DoctorSchedule>> getWeeklyScheduleGrouped(String doctorId, LocalDate weekStart) {
        List<DoctorSchedule> schedules = getWeeklySchedule(doctorId, weekStart);
        // 按日期分组，并保持日期顺序
        return schedules.stream()
                .collect(Collectors.groupingBy(
                        DoctorSchedule::getWorkDate,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    @Override
    public LocalDate[] getWeekRange(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        // 以周一为一周的开始（ISO 标准）
        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        return new LocalDate[]{weekStart, weekEnd};
    }
}
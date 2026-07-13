package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.doctor.entity.DoctorSchedule;
import com.cloudbrainmed.doctor.mapper.DoctorScheduleMapper;
import com.cloudbrainmed.doctor.service.DoctorScheduleService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        return schedules.stream()
                .collect(Collectors.groupingBy(
                        DoctorSchedule::getWorkDate,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    @Override
    public boolean isDoctorAvailable(String doctorId, LocalDateTime now) {
        if (doctorId == null || now == null) {
            return false;
        }
        boolean available = filterAvailableDoctors(List.of(doctorId), now).contains(doctorId);
        return available;
    }

    @Override
    public Set<String> filterAvailableDoctors(List<String> doctorIds, LocalDateTime now) {
        if (doctorIds == null || doctorIds.isEmpty() || now == null) {
            return Set.of();
        }
        LocalDate workDate = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();
        List<DoctorSchedule> schedules = scheduleMapper.selectPublishedByDoctorIdsAndDate(doctorIds, workDate);
        Set<String> result = schedules.stream()
                .filter(schedule -> schedule.getStartTime() != null && schedule.getEndTime() != null)
                .filter(schedule -> !schedule.getStartTime().isAfter(schedule.getEndTime()))
                .filter(schedule -> !currentTime.isBefore(schedule.getStartTime()) && !currentTime.isAfter(schedule.getEndTime()))
                .map(DoctorSchedule::getDoctorId)
                .collect(Collectors.toSet());
        return result;
    }

    @Override
    public LocalDate[] getWeekRange(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        return new LocalDate[]{weekStart, weekEnd};
    }
}

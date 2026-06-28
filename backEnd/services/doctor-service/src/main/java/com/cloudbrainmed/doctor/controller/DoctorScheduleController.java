package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.cloudbrainmed.doctor.entity.DoctorSchedule;
import com.cloudbrainmed.doctor.service.DoctorScheduleService;
import com.cloudbrainmed.doctor.vo.WeeklyScheduleVo;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/doctor-service/schedule")
public class DoctorScheduleController {

    private final DoctorScheduleService scheduleService;

    public DoctorScheduleController(DoctorScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /**
     * 查询医生本周排班（一周视图）
     */
    @GetMapping("/weekly")
    public Result<?> getWeeklySchedule(
            @RequestHeader(value = "token", required = true) String token,
            @RequestParam(required = false) String weekStart) {

        String doctorId = extractDoctorId(token);
        LocalDate targetDate = weekStart != null && !weekStart.isEmpty()
                ? LocalDate.parse(weekStart)
                : LocalDate.now();

        LocalDate[] range = scheduleService.getWeekRange(targetDate);
        Map<LocalDate, List<DoctorSchedule>> grouped = scheduleService.getWeeklyScheduleGrouped(doctorId, targetDate);

        // 构建完整的一周数据（包括没有排班的日子）
        List<WeeklyScheduleVo.WeekDayVo> weekData = new ArrayList<>();
        for (LocalDate date = range[0]; !date.isAfter(range[1]); date = date.plusDays(1)) {
            WeeklyScheduleVo.WeekDayVo dayVo = new WeeklyScheduleVo.WeekDayVo();
            dayVo.setDate(date);
            dayVo.setDayOfWeek(date.getDayOfWeek().getValue());
            dayVo.setDayName(getChineseDayName(date.getDayOfWeek()));
            dayVo.setSchedules(grouped.getOrDefault(date, Collections.emptyList()));
            weekData.add(dayVo);
        }

        WeeklyScheduleVo vo = new WeeklyScheduleVo();
        vo.setWeekStart(range[0]);
        vo.setWeekEnd(range[1]);
        vo.setWeekData(weekData);

        return Result.ok(vo);
    }

    /**
     * 获取周中某一天的中文名称
     */
    private String getChineseDayName(DayOfWeek dayOfWeek) {
        Map<DayOfWeek, String> map = new LinkedHashMap<>();
        map.put(DayOfWeek.MONDAY, "周一");
        map.put(DayOfWeek.TUESDAY, "周二");
        map.put(DayOfWeek.WEDNESDAY, "周三");
        map.put(DayOfWeek.THURSDAY, "周四");
        map.put(DayOfWeek.FRIDAY, "周五");
        map.put(DayOfWeek.SATURDAY, "周六");
        map.put(DayOfWeek.SUNDAY, "周日");
        return map.get(dayOfWeek);
    }

    /**
     * 从 Token 中提取医生 ID
     */
    private String extractDoctorId(String token) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("未登录，请先登录");
        }
        try {
            return DoctorJwtUtil.getUserId(token);
        } catch (Exception e) {
            throw new RuntimeException("Token 无效，请重新登录");
        }
    }
}
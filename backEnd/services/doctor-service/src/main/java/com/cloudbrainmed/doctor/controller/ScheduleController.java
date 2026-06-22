package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.cloudbrainmed.doctor.service.ScheduleService;
import org.springframework.web.bind.annotation.*;

/**
 * 医生排班控制器（对应 API 4.4.4）
 */
@RestController
@RequestMapping("/doctor-service/schedule")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /** 查询我的排班列表 */
    @GetMapping("/my-list")
    public Result<?> myList(@RequestHeader(value = "token", required = false) String token) {
        String doctorId = extractDoctorId(token);
        return Result.ok(scheduleService.getByDoctorId(doctorId));
    }

    /** 查询我未来的排班 */
    @GetMapping("/upcoming")
    public Result<?> upcoming(@RequestHeader(value = "token", required = false) String token) {
        String doctorId = extractDoctorId(token);
        return Result.ok(scheduleService.getUpcoming(doctorId));
    }

    private String extractDoctorId(String token) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("未登录，请先登录");
        }
        try {
            return DoctorJwtUtil.getUserId(token);
        } catch (Exception e) {
            return token;
        }
    }
}
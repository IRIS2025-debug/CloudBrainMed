package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.cloudbrainmed.doctor.service.ExamOrderService;
import org.springframework.web.bind.annotation.*;

/**
 * 检查申请单控制器
 */
@RestController
@RequestMapping("/doctor-service/exam-order")
public class ExamOrderController {

    private final ExamOrderService examOrderService;

    public ExamOrderController(ExamOrderService examOrderService) {
        this.examOrderService = examOrderService;
    }

    /** 按挂号ID查询检查单 */
    @GetMapping("/list")
    public Result<?> listByRegisterId(@RequestParam String registerId, @RequestParam String doctorId) {
        return Result.ok(examOrderService.getByRegisterId(registerId, doctorId));
    }

    /** 查询我开出的所有检查单 */
    @GetMapping("/my-list")
    public Result<?> myList(@RequestHeader(value = "token", required = false) String token) {
        String doctorId = extractDoctorId(token);
        return Result.ok(examOrderService.getByDoctorId(doctorId));
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
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
    public Result<?> listByRegisterId(@RequestHeader(value = "token", required = false) String token,
                                      @RequestParam String registerId,
                                      @RequestParam(required = false) String doctorId) {
        doctorId = extractDoctorId(token);
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
        Integer roleType;
        String doctorId;
        try {
            roleType = DoctorJwtUtil.getRoleType(token);
            doctorId = DoctorJwtUtil.getUserId(token);
        } catch (Exception exception) {
            throw new RuntimeException("医生登录凭证无效");
        }
        if (!Integer.valueOf(2).equals(roleType)) {
            throw new RuntimeException("仅医生可以查看检查检验申请");
        }
        return doctorId;
    }
}

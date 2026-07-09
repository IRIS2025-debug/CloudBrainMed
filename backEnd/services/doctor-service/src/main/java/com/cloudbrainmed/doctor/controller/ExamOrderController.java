package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.cloudbrainmed.doctor.dto.SaveReportRequestDto;
import com.cloudbrainmed.doctor.entity.MedicalReport;
import com.cloudbrainmed.doctor.service.ExamOrderService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
/**
 * 检查申请单控制器
 */
@Slf4j
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

    /**
     * 保存CT检查报告
     */
    @PostMapping("/report/save")
    public Result<MedicalReport> saveReport(@Valid @RequestBody SaveReportRequestDto request) {
        log.info("========== 开始保存CT检查报告 ==========");
        log.info("挂号ID: {}", request.getRegisterId());

        try {
            // 参数校验
            if (request.getRegisterId() == null || request.getRegisterId().isEmpty()) {
                log.warn("挂号ID为空");
                return Result.error(400, "挂号ID不能为空");
            }
            if (request.getComprehensive() == null) {
                log.warn("综合报告内容为空");
                return Result.error(400, "报告内容不能为空");
            }

            // 调用Service保存
            MedicalReport savedReport = examOrderService.saveReport(request);
            log.info("报告保存成功，报告ID: {}", savedReport.getReportId());

            return Result.success("报告保存成功", savedReport);

        } catch (IllegalArgumentException e) {
            log.error("参数校验失败: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("保存报告异常: ", e);
            return Result.error("报告保存失败: " + e.getMessage());
        }
    }
}

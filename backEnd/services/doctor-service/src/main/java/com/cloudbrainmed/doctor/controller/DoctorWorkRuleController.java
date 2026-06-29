package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.cloudbrainmed.doctor.dto.DoctorWorkRuleRequest;
import com.cloudbrainmed.doctor.service.DoctorWorkRuleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/doctor-service/schedule/work-rules")
public class DoctorWorkRuleController {

    private final DoctorWorkRuleService workRuleService;

    public DoctorWorkRuleController(
            DoctorWorkRuleService workRuleService) {
        this.workRuleService = workRuleService;
    }

    @GetMapping
    public Result<?> list(@RequestHeader("token") String token) {
        return Result.ok(workRuleService.list(extractDoctorId(token)));
    }

    @PostMapping
    public Result<?> create(
            @RequestHeader("token") String token,
            @Valid @RequestBody DoctorWorkRuleRequest request) {
        return Result.ok(workRuleService.create(
                request, extractDoctorId(token)));
    }

    @PutMapping("/{ruleId}")
    public Result<?> update(
            @RequestHeader("token") String token,
            @PathVariable String ruleId,
            @Valid @RequestBody DoctorWorkRuleRequest request) {
        return Result.ok(workRuleService.update(
                ruleId, request, extractDoctorId(token)));
    }

    @DeleteMapping("/{ruleId}")
    public Result<?> disable(
            @RequestHeader("token") String token,
            @PathVariable String ruleId) {
        workRuleService.disable(ruleId, extractDoctorId(token));
        return Result.ok();
    }

    private String extractDoctorId(String token) {
        try {
            if (!Integer.valueOf(2).equals(
                    DoctorJwtUtil.getRoleType(token))) {
                throw new IllegalArgumentException(
                        "仅医生可以维护工作时间");
            }
            return DoctorJwtUtil.getUserId(token);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("医生登录凭证无效");
        }
    }
}

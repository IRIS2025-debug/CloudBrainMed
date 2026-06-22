package com.cloudbrainmed.patient.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.patient.module5.service.PatientProfileService;
import com.cloudbrainmed.patient.service.RegisterService;
import org.springframework.web.bind.annotation.*;

/**
 * 患者信息控制器（内部 Feign 调用专用）
 */
@RestController
@RequestMapping("/patient-service")
public class PatientProfileController {

    private final PatientProfileService profileService;
    private final RegisterService registerService;

    public PatientProfileController(PatientProfileService profileService, RegisterService registerService) {
        this.profileService = profileService;
        this.registerService = registerService;
    }

    /** 按患者ID查基本信息（模块四挂号/模块一AI分诊用） */
    @GetMapping("/info/{patientId}")
    public Result<?> getPatientInfo(@PathVariable String patientId) {
        return Result.ok(profileService.getInfo(patientId));
    }

    /** 按患者ID查挂号记录（模块四挂号用） */
    @GetMapping("/register/history/{patientId}")
    public Result<?> getRegisterHistory(@PathVariable String patientId) {
        return Result.ok(registerService.getRegisterHistory(patientId));
    }
}
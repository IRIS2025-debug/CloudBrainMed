package com.cloudbrainmed.patient.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.patient.service.MedicalRecordService;
import org.springframework.web.bind.annotation.*;

/**
 * 患者病历控制器（对应 API 4.3.3）
 */
@RestController
@RequestMapping("/patient-service/medical")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    public MedicalRecordController(MedicalRecordService medicalRecordService) {
        this.medicalRecordService = medicalRecordService;
    }

    /** 按挂号ID查询病历 */
    @GetMapping("/list")
    public Result<?> listByRegisterId(@RequestParam String registerId) {
        return Result.ok(medicalRecordService.getByRegisterId(registerId));
    }

    /** 按患者ID查询所有病历 */
    @GetMapping("/my-list")
    public Result<?> listByPatientId(@RequestParam String patientId) {
        return Result.ok(medicalRecordService.getByPatientId(patientId));
    }
}
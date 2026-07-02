package com.cloudbrainmed.patient.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.JwtUtil;
import com.cloudbrainmed.patient.service.MedicalRecordService;
import org.springframework.web.bind.annotation.*;

/**
 * 患者病历控制器（对应 API 4.3.3）
 */
@RestController
@RequestMapping("/patient-service/medical")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;
    private final JwtUtil jwtUtil;

    public MedicalRecordController(MedicalRecordService medicalRecordService, JwtUtil jwtUtil) {
        this.medicalRecordService = medicalRecordService;
        this.jwtUtil = jwtUtil;
    }

    /** 按挂号ID查询病历 */
    @GetMapping("/list")
    public Result<?> listByRegisterId(@RequestHeader(value = "token", required = false) String token,
                                      @RequestParam String registerId) {
        String patientId = extractPatientId(token);
        return Result.ok(medicalRecordService.getByRegisterId(registerId, patientId));
    }

    /** 按患者ID查询所有病历 */
    @GetMapping("/my-list")
    public Result<?> listByPatientId(@RequestHeader(value = "token", required = false) String token,
                                     @RequestParam(required = false) String patientId) {
        patientId = extractPatientId(token);
        return Result.ok(medicalRecordService.getByPatientId(patientId));
    }

    private String extractPatientId(String token) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("未登录，请先登录");
        }
        try {
            return jwtUtil.getPatientIdFromToken(token);
        } catch (Exception e) {
            throw new RuntimeException("Token 无效，请重新登录");
        }
    }
}

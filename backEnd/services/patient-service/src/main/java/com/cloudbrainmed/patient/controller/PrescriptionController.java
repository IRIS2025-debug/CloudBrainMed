package com.cloudbrainmed.patient.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.JwtUtil;
import com.cloudbrainmed.patient.service.PrescriptionService;
import org.springframework.web.bind.annotation.*;

/**
 * 患者处方控制器（对应 API 4.3.3）
 */
@RestController
@RequestMapping("/patient-service/prescription")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final JwtUtil jwtUtil;

    public PrescriptionController(PrescriptionService prescriptionService, JwtUtil jwtUtil) {
        this.prescriptionService = prescriptionService;
        this.jwtUtil = jwtUtil;
    }

    /** 按挂号ID查询处方 */
    @GetMapping("/list")
    public Result<?> listByRegisterId(@RequestHeader(value = "token", required = false) String token,
                                      @RequestParam String registerId) {
        String patientId = extractPatientId(token);
        return Result.ok(prescriptionService.getByRegisterId(registerId, patientId));
    }

    /** 按患者ID查询所有处方 */
    @GetMapping("/my-list")
    public Result<?> listByPatientId(@RequestHeader(value = "token", required = false) String token,
                                     @RequestParam(required = false) String patientId) {
        patientId = extractPatientId(token);
        return Result.ok(prescriptionService.getByPatientId(patientId));
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

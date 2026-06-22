package com.cloudbrainmed.patient.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.patient.service.PrescriptionService;
import org.springframework.web.bind.annotation.*;

/**
 * 患者处方控制器（对应 API 4.3.3）
 */
@RestController
@RequestMapping("/patient-service/prescription")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    /** 按挂号ID查询处方 */
    @GetMapping("/list")
    public Result<?> listByRegisterId(@RequestParam String registerId) {
        return Result.ok(prescriptionService.getByRegisterId(registerId));
    }

    /** 按患者ID查询所有处方 */
    @GetMapping("/my-list")
    public Result<?> listByPatientId(@RequestParam String patientId) {
        return Result.ok(prescriptionService.getByPatientId(patientId));
    }
}
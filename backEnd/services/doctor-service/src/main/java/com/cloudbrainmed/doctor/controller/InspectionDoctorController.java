package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.doctor.service.MedicalOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inspection-doctor")
@RequiredArgsConstructor
public class InspectionDoctorController {

    private final MedicalOrderService medicalOrderService;

    @GetMapping("/lab-orders")
    public Result<?> getAllLabOrders() {
        return Result.ok(medicalOrderService.getAllLabOrders());
    }

    @GetMapping("/order/{orderId}")
    public Result<?> getByOrderId(@PathVariable String orderId) {
        return Result.ok(medicalOrderService.getByOrderId(orderId));
    }
}

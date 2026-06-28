package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.doctor.service.InspectionOrderService;
import org.springframework.web.bind.annotation.*;

/**
 * 检验医生控制器 — 查看所有检验申请（medical_order 表，item_category=LAB）
 */
@RestController
@RequestMapping("/doctor-service/inspection-doctor")
public class InspectionDoctorController {

    private final InspectionOrderService inspectionOrderService;

    public InspectionDoctorController(InspectionOrderService inspectionOrderService) {
        this.inspectionOrderService = inspectionOrderService;
    }

    /** 查看所有检验申请列表 */
    @GetMapping("/order/list")
    public Result<?> list() {
        return Result.ok(inspectionOrderService.getAllLabOrders());
    }

    /** 查看检验申请详情 */
    @GetMapping("/order/detail/{orderId}")
    public Result<?> detail(@PathVariable String orderId) {
        return Result.ok(inspectionOrderService.getByOrderId(orderId));
    }
}

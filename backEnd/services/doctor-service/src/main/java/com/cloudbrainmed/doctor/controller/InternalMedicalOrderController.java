package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.doctor.service.TaskSchedulerService;
import com.cloudbrainmed.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 内部接口 — 供 payment-service 支付成功后回调
 * 路径 /internal/** 由 Gateway 屏蔽外部访问，仅服务间可调
 */
@Slf4j
@RestController
@RequestMapping("/doctor-service/internal/medical-order")
public class InternalMedicalOrderController {

    private final TaskSchedulerService taskSchedulerService;

    public InternalMedicalOrderController(TaskSchedulerService taskSchedulerService) {
        this.taskSchedulerService = taskSchedulerService;
    }

    /**
     * 支付成功回调
     * 当患者支付完成后，payment-service 调用此接口触发入队操作
     * ========================
     * 业务逻辑：
     * 1. 更新 medical_order.pay_status = paid
     * 2. 将所有 medical_order_item 从 waiting_assign → queued
     * 3. 幂等保证：重复调用只处理一次
     */
    @PostMapping("/payment-success/{orderId}")
    public Result<Boolean> onPaymentSuccess(@PathVariable String orderId) {
        log.info("Received payment success callback for order: {}", orderId);
        try {
            taskSchedulerService.enqueueByPayment(orderId);
            return Result.ok(true);
        } catch (BusinessException e) {
            log.warn("Payment callback business error: {}", e.getMessage());
            return Result.ok(false);
        } catch (Exception e) {
            log.error("Payment callback error: {}", e.getMessage(), e);
            return Result.ok(false);
        }
    }
}
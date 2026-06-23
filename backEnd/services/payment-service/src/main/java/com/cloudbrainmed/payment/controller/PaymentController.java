package com.cloudbrainmed.payment.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.payment.service.PaymentService;
import org.springframework.web.bind.annotation.*;

/**
 * 支付服务控制器
 */
@RestController
@RequestMapping("/payment-service")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** 查询患者缴费记录 */
    @GetMapping("/history/{patientId}")
    public Result<?> getPaymentHistory(@PathVariable String patientId) {
        return Result.ok(paymentService.getPaymentHistory(patientId));
    }
}
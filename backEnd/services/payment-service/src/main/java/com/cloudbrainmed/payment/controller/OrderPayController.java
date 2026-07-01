package com.cloudbrainmed.payment.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.payment.service.OrderPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 医疗订单支付控制器
 * 处理 order 层面的支付成功回调
 * ========================
 * 幂等设计：通过数据库 WHERE pay_status = 'WAITING' 保证重复调用不会重复处理
 */
@Slf4j
@RestController
@RequestMapping("/payment-service/order")
public class OrderPayController {

    private final OrderPayService orderPayService;

    public OrderPayController(OrderPayService orderPayService) {
        this.orderPayService = orderPayService;
    }

    /**
     * 订单支付成功回调
     * 请求参数：{ "orderId": "xxx", "payStatus": "paid" }
     * ========================
     * 业务逻辑：
     * 1. 更新 pay 表状态
     * 2. 调用 doctor-service 更新 medical_order.pay_status = paid
     * 3. 将 medical_order_item 从 waiting_assign → queued
     */
    @PostMapping("/pay/success")
    public Result<Map<String, Object>> paySuccess(@RequestBody Map<String, String> body) {
        String orderId = body.get("orderId");
        String payStatus = body.get("payStatus");

        if (orderId == null || orderId.isBlank()) {
            return Result.error("orderId 不能为空");
        }
        if (!"paid".equalsIgnoreCase(payStatus)) {
            return Result.error("payStatus 必须为 paid");
        }

        log.info("Order payment success: orderId={}", orderId);
        boolean success = orderPayService.handleOrderPaySuccess(orderId);
        return Result.ok(Map.of(
                "orderId", orderId,
                "success", success,
                "message", success ? "已入队" : "订单不存在或已处理"
        ));
    }
}
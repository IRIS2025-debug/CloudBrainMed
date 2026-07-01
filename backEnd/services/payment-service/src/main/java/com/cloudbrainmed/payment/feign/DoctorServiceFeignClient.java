package com.cloudbrainmed.payment.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

/**
 * 支付服务内部 Feign — 调用 doctor-service 的支付成功回调
 * 不依赖 service-api 模块，避免循环依赖
 */
@FeignClient(name = "doctor-service")
public interface DoctorServiceFeignClient {

    @PostMapping("/doctor-service/internal/medical-order/payment-success/{orderId}")
    Map<String, Object> onPaymentSuccess(@PathVariable("orderId") String orderId);
}
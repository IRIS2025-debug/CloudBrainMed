package com.cloudbrainmed.api.feign;

import com.cloudbrainmed.api.fallback.PaymentFeignFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "payment-service", fallback = PaymentFeignFallback.class)
public interface PaymentFeignClient {

    /** 查询患者缴费记录 */
    @GetMapping("/payment-service/history/{patientId}")
    Map<String, Object> getPaymentHistory(@PathVariable("patientId") String patientId);
}
package com.cloudbrainmed.payment.service.impl;

import com.cloudbrainmed.payment.service.MedicalOrderCallbackService;
import com.cloudbrainmed.payment.feign.DoctorServiceFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 医疗订单支付成功回调实现
 * 通过 OpenFeign 调用 doctor-service 触发入队操作
 * 保证幂等性：只有第一次有效处理有效
 */
@Slf4j
@Service
public class MedicalOrderCallbackServiceImpl implements MedicalOrderCallbackService {

    private final DoctorServiceFeignClient doctorServiceFeignClient;

    public MedicalOrderCallbackServiceImpl(DoctorServiceFeignClient doctorServiceFeignClient) {
        this.doctorServiceFeignClient = doctorServiceFeignClient;
    }

    @Override
    public boolean onOrderPaid(String orderId) {
        log.info("Medical order paid callback: orderId={}", orderId);
        try {
            Map<String, Object> result = doctorServiceFeignClient.onPaymentSuccess(orderId);
            Boolean success = (Boolean) result.get("success");
            log.info("Payment callback result: success={}", success);
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            log.error("Failed to call medical order payment callback: {}", e.getMessage(), e);
            return false;
        }
    }
}
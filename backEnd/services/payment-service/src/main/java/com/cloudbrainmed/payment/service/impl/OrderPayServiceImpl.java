package com.cloudbrainmed.payment.service.impl;

import com.cloudbrainmed.payment.service.MedicalOrderCallbackService;
import com.cloudbrainmed.payment.service.OrderPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单支付服务实现
 * ========================
 * 幂等保证：
 * - pay 表：WHERE pay_status = 'WAITING' 防重复
 * - medical_order 表：通过 doctor-service 的 WHERE pay_status = 'WAITING' 防重复
 * - medical_order_item 表：WHERE status = 'WAITING_ASSIGN' 防重复
 */
@Slf4j
@Service
public class OrderPayServiceImpl implements OrderPayService {

    private final MedicalOrderCallbackService medicalOrderCallbackService;

    public OrderPayServiceImpl(MedicalOrderCallbackService medicalOrderCallbackService) {
        this.medicalOrderCallbackService = medicalOrderCallbackService;
    }

    @Override
    public boolean handleOrderPaySuccess(String orderId) {
        log.info("Handling order payment success: orderId={}", orderId);
        return medicalOrderCallbackService.onOrderPaid(orderId);
    }
}
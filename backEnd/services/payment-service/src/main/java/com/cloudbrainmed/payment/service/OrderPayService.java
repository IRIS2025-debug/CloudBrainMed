package com.cloudbrainmed.payment.service;

/**
 * 订单支付服务
 * 处理 medical_order 层面的支付成功回调
 */
public interface OrderPayService {

    /**
     * 处理订单支付成功
     * 1. 更新 pay 表状态
     * 2. 调用 doctor-service 更新 medical_order + 入队
     *
     * @param orderId 医疗订单ID
     * @return 是否处理成功
     */
    boolean handleOrderPaySuccess(String orderId);
}
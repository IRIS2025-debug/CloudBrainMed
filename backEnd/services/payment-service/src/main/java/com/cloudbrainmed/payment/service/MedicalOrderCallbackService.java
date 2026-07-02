package com.cloudbrainmed.payment.service;

/**
 * 支付成功回调服务接口
 * 当医疗订单支付成功后，触发后台入队操作
 */
public interface MedicalOrderCallbackService {

    /**
     * 医疗订单支付成功回调
     * @param orderId 医疗订单ID
     * @return 是否处理成功
     */
    boolean onOrderPaid(String orderId);
}
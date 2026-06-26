package com.cloudbrainmed.payment.service;

import com.cloudbrainmed.payment.dto.PayQueryDto;
import com.cloudbrainmed.payment.dto.UnifiedPayDto;
import com.cloudbrainmed.payment.vo.PayResultVo;
import com.cloudbrainmed.common.result.PageResult;

import java.util.List;

public interface PayService {

    /**
     * 创建支付订单
     */
    PayResultVo createPayOrder(UnifiedPayDto dto);

    /**
     * 支付成功回调
     */
    boolean paySuccess(String payId);

    /**
     * 取消支付
     */
    boolean cancelPay(String payId);

    /**
     * 退款
     */
    boolean refundPay(String payId);

    /**
     * 根据支付ID查询
     */
    PayResultVo getByPayId(String payId);

    /**
     * 根据业务ID查询
     */
    PayResultVo getByBusinessId(String businessId, String orderType);

    /**
     * 查询患者所有支付记录
     */
    List<PayResultVo> getPayHistory(String patientId);

    /**
     * 分页查询支付记录
     */
    PageResult<PayResultVo> queryPayPage(PayQueryDto dto);
}
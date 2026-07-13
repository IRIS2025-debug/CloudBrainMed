package com.cloudbrainmed.api.feign;

import com.cloudbrainmed.api.fallback.PaymentFeignFallback;
import com.cloudbrainmed.common.result.PageResult;
import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.payment.dto.PayQueryDto;
import com.cloudbrainmed.payment.dto.UnifiedPayDto;
import com.cloudbrainmed.payment.vo.PayResultVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "payment-service", fallback = PaymentFeignFallback.class)
public interface PaymentFeignClient {

    /**
     * 创建支付订单
     */
    @PostMapping("/payment-service/pay/create")
    Result<PayResultVo> createPayOrder(@RequestBody UnifiedPayDto dto);

    /**
     * 支付成功回调
     */
    @PostMapping("/payment-service/pay/success/{payId}")
    Result<Boolean> paySuccess(@PathVariable("payId") String payId);

    /**
     * 取消支付
     */
    @PostMapping("/payment-service/pay/cancel/{payId}")
    Result<Boolean> cancelPay(@PathVariable("payId") String payId);

    /**
     * 退款
     */
    @PostMapping("/payment-service/pay/refund/{payId}")
    Result<Boolean> refundPay(@PathVariable("payId") String payId);

    /**
     * 申请退款（带业务状态校验）
     */
    @PostMapping("/payment-service/pay/refund/apply/{payId}")
    Result<Boolean> applyRefund(@PathVariable("payId") String payId);

    /**
     * 根据支付ID查询
     */
    @GetMapping("/payment-service/pay/{payId}")
    Result<PayResultVo> getByPayId(@PathVariable("payId") String payId);

    /**
     * 根据业务ID查询
     */
    @GetMapping("/payment-service/pay/business")
    Result<PayResultVo> getByBusinessId(@RequestParam("businessId") String businessId,
                                        @RequestParam("orderType") String orderType);

    /**
     * 查询患者支付历史（普通版）
     */
    @GetMapping("/payment-service/pay/history/{patientId}")
    Result<List<PayResultVo>> getPayHistory(@PathVariable("patientId") String patientId);

    /**
     * 查询患者支付历史（增强版：自动处理过期 + 退款状态）
     */
    @GetMapping("/payment-service/pay/history-enhanced/{patientId}")
    Result<List<PayResultVo>> getPayHistoryEnhanced(@PathVariable("patientId") String patientId);

    /**
     * 分页查询支付记录
     */
    @PostMapping("/payment-service/pay/page")
    Result<PageResult<PayResultVo>> queryPayPage(@RequestBody PayQueryDto dto);
}
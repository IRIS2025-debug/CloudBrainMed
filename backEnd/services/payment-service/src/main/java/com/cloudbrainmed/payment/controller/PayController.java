package com.cloudbrainmed.payment.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.result.PageResult;
import com.cloudbrainmed.payment.dto.PayQueryDto;
import com.cloudbrainmed.payment.dto.UnifiedPayDto;
import com.cloudbrainmed.payment.service.PayService;
import com.cloudbrainmed.payment.vo.PayResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 支付服务控制器
 */
@RestController
@RequestMapping("/payment-service/pay")
public class PayController {

    private static final Logger log = LoggerFactory.getLogger(PayController.class);

    private final PayService payService;

    public PayController(PayService payService) {
        this.payService = payService;
    }

    /**
     * 创建支付订单
     */
    @PostMapping("/create")
    public Result<PayResultVo> createPayOrder(@RequestBody UnifiedPayDto dto) {
        PayResultVo result = payService.createPayOrder(dto);
        return Result.ok(result);
    }

    /**
     * 支付成功回调
     */
    @PostMapping("/success/{payId}")
    public Result<Boolean> paySuccess(@PathVariable String payId) {
        boolean result = payService.paySuccess(payId);
        return Result.ok(result);
    }

    /**
     * 取消支付
     */
    @PostMapping("/cancel/{payId}")
    public Result<Boolean> cancelPay(@PathVariable String payId) {
        boolean result = payService.cancelPay(payId);
        return Result.ok(result);
    }

    /**
     * 退款（直接退款，不校验业务状态）
     */
    @PostMapping("/refund/{payId}")
    public Result<Boolean> refundPay(@PathVariable String payId) {
        boolean result = payService.refundPay(payId);
        return Result.ok(result);
    }

    /**
     * 申请退款（带业务状态校验）
     * 只有已支付且业务表也是已支付状态才能退款
     */
    @PostMapping("/refund/apply/{payId}")
    public Result<Boolean> applyRefund(@PathVariable String payId) {
        // 先校验是否可退款
        if (!payService.canRefund(payId)) {
            return Result.error("当前订单状态不支持退款（仅已支付且业务状态为已支付的订单可退款）");
        }
        boolean result = payService.refundPay(payId);
        return Result.ok(result);
    }

    /**
     * 根据支付ID查询
     */
    @GetMapping("/{payId}")
    public Result<PayResultVo> getByPayId(@PathVariable String payId) {
        PayResultVo result = payService.getByPayId(payId);
        return Result.ok(result);
    }

    /**
     * 根据业务ID查询
     */
    @GetMapping("/business")
    public Result<PayResultVo> getByBusinessId(@RequestParam String businessId,
                                               @RequestParam String orderType) {
        PayResultVo result = payService.getByBusinessId(businessId, orderType);
        return Result.ok(result);
    }

    /**
     * 查询患者支付历史（普通版）
     */
    @GetMapping("/history/{patientId}")
    public Result<List<PayResultVo>> getPayHistory(@PathVariable String patientId) {
        List<PayResultVo> result = payService.getPayHistory(patientId);
        return Result.ok(result);
    }

    /**
     * 查询患者支付历史（增强版：自动处理过期订单 + 退款状态）
     * 前端调用此接口，后端自动处理过期订单取消和退款资格判断
     */
    @GetMapping("/history-enhanced/{patientId}")
    public Result<List<PayResultVo>> getPayHistoryEnhanced(@PathVariable String patientId) {
        log.info("增强版查询支付历史: patientId={}", patientId);
        List<PayResultVo> result = payService.getPayHistoryWithStatus(patientId);
        log.info("增强版查询返回: {} 条记录", result.size());
        return Result.ok(result);
    }

    /**
     * 分页查询支付记录
     */
    @PostMapping("/page")
    public Result<PageResult<PayResultVo>> queryPayPage(@RequestBody PayQueryDto dto) {
        PageResult<PayResultVo> result = payService.queryPayPage(dto);
        return Result.ok(result);
    }
}
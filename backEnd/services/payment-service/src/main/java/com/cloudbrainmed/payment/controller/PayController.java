package com.cloudbrainmed.payment.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.result.PageResult;
import com.cloudbrainmed.payment.dto.PayQueryDto;
import com.cloudbrainmed.payment.dto.UnifiedPayDto;
import com.cloudbrainmed.payment.service.PayService;
import com.cloudbrainmed.payment.vo.PayResultVo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 支付服务控制器
 */
@RestController
@RequestMapping("/payment-service/pay")
public class PayController {

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
     * 退款
     */
    @PostMapping("/refund/{payId}")
    public Result<Boolean> refundPay(@PathVariable String payId) {
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
     * 查询患者支付历史
     */
    @GetMapping("/history/{patientId}")
    public Result<List<PayResultVo>> getPayHistory(@PathVariable String patientId) {
        List<PayResultVo> result = payService.getPayHistory(patientId);
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
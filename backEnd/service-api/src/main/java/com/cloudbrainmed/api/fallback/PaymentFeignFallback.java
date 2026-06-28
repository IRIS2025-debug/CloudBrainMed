// service-api/src/main/java/com/cloudbrainmed/api/fallback/PaymentFeignFallback.java
package com.cloudbrainmed.api.fallback;

import com.cloudbrainmed.api.feign.PaymentFeignClient;
import com.cloudbrainmed.common.result.PageResult;
import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.payment.dto.PayQueryDto;
import com.cloudbrainmed.payment.dto.UnifiedPayDto;
import com.cloudbrainmed.payment.vo.PayResultVo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PaymentFeignFallback implements PaymentFeignClient {

    @Override
    public Result<PayResultVo> createPayOrder(UnifiedPayDto dto) {
        return Result.error("支付服务不可用，请稍后重试");
    }

    @Override
    public Result<Boolean> paySuccess(String payId) {
        return Result.error("支付服务不可用，请稍后重试");
    }

    @Override
    public Result<Boolean> cancelPay(String payId) {
        return Result.error("支付服务不可用，请稍后重试");
    }

    @Override
    public Result<Boolean> refundPay(String payId) {
        return Result.error("支付服务不可用，请稍后重试");
    }

    @Override
    public Result<PayResultVo> getByPayId(String payId) {
        return Result.error("支付服务不可用，请稍后重试");
    }

    @Override
    public Result<PayResultVo> getByBusinessId(String businessId, String orderType) {
        return Result.error("支付服务不可用，请稍后重试");
    }

    @Override
    public Result<List<PayResultVo>> getPayHistory(String patientId) {
        return Result.ok(new ArrayList<>());
    }

    @Override
    public Result<PageResult<PayResultVo>> queryPayPage(PayQueryDto dto) {
        PageResult<PayResultVo> emptyPage = new PageResult<>(new ArrayList<>(), 0L, dto.getPageNum(), dto.getPageSize());
        return Result.ok(emptyPage);
    }
}
package com.cloudbrainmed.api.feign;

import com.cloudbrainmed.api.fallback.DoctorFeignFallback;
import com.cloudbrainmed.api.dto.ReportContextDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 医生服务 Feign 接口（供模块二 AI 辅助接诊/排班 调用）
 */
@FeignClient(name = "doctor-service", fallback = DoctorFeignFallback.class)
public interface DoctorFeignClient {

    /** 查询接诊患者列表（AI 辅助接诊用） */
    @GetMapping("/doctor-service/consult/list")
    Map<String, Object> getConsultList(@RequestParam Map<String, String> params);

    /** 获取接诊详情（含患者信息+病历，AI 辅助接诊用） */
    @GetMapping("/doctor-service/consult/detail")
    Map<String, Object> getConsultDetail(@RequestParam("registerId") String registerId);

    /** 查询医生排班（AI 智能排班用） */
    @GetMapping("/doctor-service/schedule/{doctorId}")
    Map<String, Object> getDoctorSchedules(@PathVariable("doctorId") String doctorId);

    /** AI服务获取可信接诊上下文，只供服务间调用。 */
    @GetMapping("/internal/doctor/consult/context")
    ReportContextDto getConsultContext(
            @RequestParam("registerId") String registerId,
            @RequestHeader("X-Doctor-Id") String doctorId,
            @RequestHeader("X-Internal-Service-Key") String serviceKey);

    /** 支付成功回调 — 触发 medical_order_item 入队 */
    @PostMapping("/doctor-service/internal/medical-order/payment-success/{orderId}")
    Map<String, Object> onPaymentSuccess(@PathVariable("orderId") String orderId);
}
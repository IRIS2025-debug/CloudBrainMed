package com.cloudbrainmed.api.feign;

import com.cloudbrainmed.api.fallback.AiFeignFallback;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "ai-service", fallback = AiFeignFallback.class)
public interface AiFeignClient {
    // AI 接入诊分析的 Feign 接口已移除，改为前端直调 ai-service。
    // ai-service 通过 DoctorFeignClient 反调 doctor-service 获取病历上下文。
}

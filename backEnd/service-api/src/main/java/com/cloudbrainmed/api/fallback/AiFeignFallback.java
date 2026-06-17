package com.cloudbrainmed.api.fallback;

import com.cloudbrainmed.api.feign.AiFeignClient;
import org.springframework.stereotype.Component;

@Component
public class AiFeignFallback implements AiFeignClient {
    // analyzeConsult 已移除，不再需要 fallback。
    // ai-service 现在通过 DoctorFeignClient 反调 doctor-service。
}

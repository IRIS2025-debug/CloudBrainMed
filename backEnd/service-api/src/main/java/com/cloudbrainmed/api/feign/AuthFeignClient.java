package com.cloudbrainmed.api.feign;

import com.cloudbrainmed.api.fallback.AuthFeignFallback;
import com.cloudbrainmed.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "auth-service", fallback = AuthFeignFallback.class)
public interface AuthFeignClient {

    @PostMapping("/auth-service/patient/verify-code")
    Result<Map<String, Object>> verifyPatientCode(@RequestBody Map<String, String> request);
}

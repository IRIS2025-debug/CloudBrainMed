package com.cloudbrainmed.api.fallback;

import com.cloudbrainmed.api.feign.AuthFeignClient;
import com.cloudbrainmed.common.result.Result;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AuthFeignFallback implements AuthFeignClient {

    @Override
    public Result<Map<String, Object>> verifyPatientCode(Map<String, String> request) {
        return Result.error(503, "认证服务暂不可用");
    }
}

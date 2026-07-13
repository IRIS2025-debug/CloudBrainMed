package com.cloudbrainmed.api.fallback;

import com.cloudbrainmed.api.feign.PatientFeignClient;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Component
public class PatientFeignFallback implements PatientFeignClient {

    @Override
    public Map<String, Object> getPatientInfo(String patientId) {
        return Map.of("error", "患者服务暂不可用");
    }

    @Override
    public Map<String, Object> getRegisterHistory(String patientId) {
        return Map.of("list", Collections.emptyList(), "total", 0);
    }

    @Override
    public Map<String, Object> getRegisterStatus(String registerId) {
        return Map.of("error", "患者服务暂不可用", "status", "UNKNOWN");
    }
}

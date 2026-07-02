package com.cloudbrainmed.api.fallback;

import com.cloudbrainmed.api.feign.DoctorFeignClient;
import com.cloudbrainmed.api.dto.ReportContextDto;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Component
public class DoctorFeignFallback implements DoctorFeignClient {

    @Override
    public Map<String, Object> getConsultList(Map<String, String> params) {
        return Map.of("list", Collections.emptyList(), "total", 0);
    }

    @Override
    public Map<String, Object> getConsultDetail(String registerId) {
        return Map.of("error", "医生服务暂不可用");
    }

    @Override
    public Map<String, Object> getDoctorSchedules(String doctorId) {
        return Map.of("schedules", Collections.emptyList());
    }

    @Override
    public ReportContextDto getConsultContext(
            String registerId, String doctorId, String serviceKey) {
        ReportContextDto context = new ReportContextDto();
        context.setAvailable(false);
        context.setErrorMessage("医生服务暂不可用，无法获取患者接诊信息");
        context.setRegisterId(registerId);
        return context;
    }

    @Override
    public Map<String, Object> onPaymentSuccess(String orderId) {
        return Map.of("success", false, "error", "doctor-service 暂不可用");
    }
}
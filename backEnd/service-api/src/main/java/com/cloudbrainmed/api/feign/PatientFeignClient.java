package com.cloudbrainmed.api.feign;

import com.cloudbrainmed.api.fallback.PatientFeignFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 患者服务 Feign 接口（供模块一 AI分诊、模块四 挂号 等调用）
 */
@FeignClient(name = "patient-service", fallback = PatientFeignFallback.class)
public interface PatientFeignClient {

    /** 查询患者基本信息 */
    @GetMapping("/patient-service/info/{patientId}")
    Map<String, Object> getPatientInfo(@PathVariable("patientId") String patientId);

    /** 查询患者挂号记录 */
    @GetMapping("/patient-service/register/history/{patientId}")
    Map<String, Object> getRegisterHistory(@PathVariable("patientId") String patientId);

    /**
     * 查询挂号状态
     * @param registerId 挂号ID
     * @return 挂号状态信息
     */
    @GetMapping("/patient-service/register/status/{registerId}")
    Map<String, Object> getRegisterStatus(@PathVariable("registerId") String registerId);
}
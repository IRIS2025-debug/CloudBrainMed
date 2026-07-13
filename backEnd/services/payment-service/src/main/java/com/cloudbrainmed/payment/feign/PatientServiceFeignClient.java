package com.cloudbrainmed.payment.feign;

import com.cloudbrainmed.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "patient-service")
public interface PatientServiceFeignClient {

    @GetMapping("/patient-service/internal/register/pay-status/{registerId}")
    Result<String> getRegisterPayStatus(@PathVariable("registerId") String registerId);

    @GetMapping("/patient-service/internal/medical-order/pay-status/{orderId}")
    Result<String> getMedicalOrderPayStatus(@PathVariable("orderId") String orderId);

    @GetMapping("/patient-service/internal/prescription/pay-status/{prescriptionId}")
    Result<String> getPrescriptionPayStatus(@PathVariable("prescriptionId") String prescriptionId);

    /**
     * 查询挂号的visit_date（就诊日期），格式: yyyy-MM-dd
     */
    @GetMapping("/patient-service/internal/register/visit-date/{registerId}")
    Result<String> getRegisterVisitDate(@PathVariable("registerId") String registerId);

    /**
     * 查询医疗订单的create_time（创建日期），格式: yyyy-MM-dd
     */
    @GetMapping("/patient-service/internal/medical-order/create-date/{orderId}")
    Result<String> getMedicalOrderCreateDate(@PathVariable("orderId") String orderId);

    /**
     * 查询处方的create_time（创建日期），格式: yyyy-MM-dd
     */
    @GetMapping("/patient-service/internal/prescription/create-date/{prescriptionId}")
    Result<String> getPrescriptionCreateDate(@PathVariable("prescriptionId") String prescriptionId);
}
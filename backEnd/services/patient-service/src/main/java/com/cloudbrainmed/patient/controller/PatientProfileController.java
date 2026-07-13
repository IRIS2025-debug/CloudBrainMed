package com.cloudbrainmed.patient.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.patient.entity.MedicalOrder;
import com.cloudbrainmed.patient.entity.Prescription;
import com.cloudbrainmed.patient.entity.Registration;
import com.cloudbrainmed.patient.module5.service.PatientProfileService;
import com.cloudbrainmed.patient.service.RegisterService;
import com.cloudbrainmed.patient.service.MedicalOrderService;
import com.cloudbrainmed.patient.service.PrescriptionService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 患者信息控制器（内部 Feign 调用专用）
 */
@RestController
@RequestMapping("/patient-service")
public class PatientProfileController {

    private final PatientProfileService profileService;
    private final RegisterService registerService;
    private final MedicalOrderService medicalOrderService;
    private final PrescriptionService prescriptionService;

    public PatientProfileController(PatientProfileService profileService,
                                    RegisterService registerService,
                                    MedicalOrderService medicalOrderService,
                                    PrescriptionService prescriptionService) {
        this.profileService = profileService;
        this.registerService = registerService;
        this.medicalOrderService = medicalOrderService;
        this.prescriptionService = prescriptionService;
    }

    /** 按患者ID查基本信息（模块四挂号/模块一AI分诊用） */
    @GetMapping("/info/{patientId}")
    public Result<?> getPatientInfo(@PathVariable String patientId) {
        return Result.ok(profileService.getInfo(patientId));
    }

    // ==================== 供 payment-service Feign 调用的业务状态查询接口 ====================

    /**
     * 查询挂号支付状态
     * 状态值: WAITING(待支付) / PAID(已支付) / CANCELLED(已取消) / REFUNDED(已退款)
     */
    @GetMapping("/internal/register/pay-status/{registerId}")
    public Result<String> getRegisterPayStatus(@PathVariable String registerId) {
        String payStatus = registerService.getPayStatus(registerId);
        return Result.ok(payStatus);
    }

    /**
     * 查询医疗订单（检查）支付状态
     * 状态值: WAITING(待支付) / PAID(已支付) / CANCELLED(已取消) / REFUNDED(已退款)
     */
    @GetMapping("/internal/medical-order/pay-status/{orderId}")
    public Result<String> getMedicalOrderPayStatus(@PathVariable String orderId) {
        String payStatus = medicalOrderService.getPayStatus(orderId);
        return Result.ok(payStatus);
    }

    /**
     * 查询处方支付状态
     * 状态值: WAITING(待支付) / PAID(已支付) / CANCELLED(已取消) / REFUNDED(已退款)
     */
    @GetMapping("/internal/prescription/pay-status/{prescriptionId}")
    public Result<String> getPrescriptionPayStatus(@PathVariable String prescriptionId) {
        String payStatus = prescriptionService.getPayStatus(prescriptionId);
        return Result.ok(payStatus);
    }

    /**
     * 查询挂号visit_date（就诊日期）
     * 格式: yyyy-MM-dd
     */
    @GetMapping("/internal/register/visit-date/{registerId}")
    public Result<String> getRegisterVisitDate(@PathVariable String registerId) {
        Registration registration = registerService.getRegisterDetail(registerId);
        if (registration == null || registration.getVisitDate() == null) {
            return Result.ok(null);
        }
        // visitDate 可能是 LocalDate 或 OffsetDateTime，统一转为 yyyy-MM-dd 字符串
        String dateStr = formatToDateString(registration.getVisitDate());
        return Result.ok(dateStr);
    }

    /**
     * 查询医疗订单create_time（创建日期）
     * 格式: yyyy-MM-dd
     */
    @GetMapping("/internal/medical-order/create-date/{orderId}")
    public Result<String> getMedicalOrderCreateDate(@PathVariable String orderId) {
        // 需要在 MedicalOrderService 中新增 getMedicalOrder 方法
        MedicalOrder order = medicalOrderService.getMedicalOrder(orderId);
        if (order == null || order.getCreateTime() == null) {
            return Result.ok(null);
        }
        String dateStr = formatToDateString(order.getCreateTime());
        return Result.ok(dateStr);
    }

    /**
     * 查询处方create_time（创建日期）
     * 格式: yyyy-MM-dd
     */
    @GetMapping("/internal/prescription/create-date/{prescriptionId}")
    public Result<String> getPrescriptionCreateDate(@PathVariable String prescriptionId) {
        // 需要在 PrescriptionService 中新增 getPrescription 方法
        Prescription prescription = prescriptionService.getPrescription(prescriptionId);
        if (prescription == null || prescription.getCreateDate() == null) {
            return Result.ok(null);
        }
        String dateStr = formatToDateString(prescription.getCreateDate());
        return Result.ok(dateStr);
    }

    // ==================== 工具方法 ====================

    /**
     * 将各种日期类型统一转换为 yyyy-MM-dd 字符串
     */
    private String formatToDateString(Object dateObj) {
        if (dateObj == null) {
            return null;
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        if (dateObj instanceof LocalDate) {
            return ((LocalDate) dateObj).format(dateFormatter);
        }
        if (dateObj instanceof OffsetDateTime) {
            return ((OffsetDateTime) dateObj).format(dateFormatter);
        }
        if (dateObj instanceof java.time.LocalDateTime) {
            return ((java.time.LocalDateTime) dateObj).format(dateFormatter);
        }

        // 兜底：尝试 toString 后截取日期部分
        String str = dateObj.toString();
        if (str.length() >= 10) {
            return str.substring(0, 10);
        }
        return str;
    }
}
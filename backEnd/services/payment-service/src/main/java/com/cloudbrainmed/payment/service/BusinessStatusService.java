package com.cloudbrainmed.payment.service;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.payment.feign.PatientServiceFeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class BusinessStatusService {

    private static final Logger log = LoggerFactory.getLogger(BusinessStatusService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final PatientServiceFeignClient patientServiceFeignClient;

    public BusinessStatusService(PatientServiceFeignClient patientServiceFeignClient) {
        this.patientServiceFeignClient = patientServiceFeignClient;
    }

    /**
     * 获取挂号支付状态
     */
    public String getRegisterPayStatus(String businessId) {
        try {
            Result<String> result = patientServiceFeignClient.getRegisterPayStatus(businessId);
            if (result != null && result.getCode() == 200) {
                return result.getData();
            }
            log.warn("查询挂号支付状态失败: businessId={}, result={}", businessId, result);
            return "UNKNOWN";
        } catch (Exception e) {
            log.error("查询挂号支付状态异常: businessId={}", businessId, e);
            return "UNKNOWN";
        }
    }

    /**
     * 获取医疗订单支付状态
     */
    public String getMedicalOrderPayStatus(String businessId) {
        try {
            Result<String> result = patientServiceFeignClient.getMedicalOrderPayStatus(businessId);
            if (result != null && result.getCode() == 200) {
                return result.getData();
            }
            log.warn("查询医疗订单支付状态失败: businessId={}, result={}", businessId, result);
            return "UNKNOWN";
        } catch (Exception e) {
            log.error("查询医疗订单支付状态异常: businessId={}", businessId, e);
            return "UNKNOWN";
        }
    }

    /**
     * 获取处方支付状态
     */
    public String getPrescriptionPayStatus(String businessId) {
        try {
            Result<String> result = patientServiceFeignClient.getPrescriptionPayStatus(businessId);
            if (result != null && result.getCode() == 200) {
                return result.getData();
            }
            log.warn("查询处方支付状态失败: businessId={}, result={}", businessId, result);
            return "UNKNOWN";
        } catch (Exception e) {
            log.error("查询处方支付状态异常: businessId={}", businessId, e);
            return "UNKNOWN";
        }
    }

    /**
     * 根据订单类型获取支付状态
     */
    public String getPayStatus(String orderType, String businessId) {
        if (businessId == null || orderType == null) {
            return "UNKNOWN";
        }

        switch (orderType) {
            case "REGISTER":
                return getRegisterPayStatus(businessId);
            case "MEDICAL":
                return getMedicalOrderPayStatus(businessId);
            case "PRESCRIPTION":
                return getPrescriptionPayStatus(businessId);
            default:
                return "UNKNOWN";
        }
    }

    // ==================== 查询业务日期（用于退款截止时间判断） ====================

    /**
     * 根据订单类型获取业务日期
     *
     * - REGISTER: 使用 visit_date（就诊日期）
     * - MEDICAL:  使用 create_time（订单创建日期）
     * - PRESCRIPTION: 使用 create_time（处方创建日期）
     *
     * 所有日期格式: yyyy-MM-dd
     *
     * @param orderType  订单类型
     * @param businessId 业务ID
     * @return 业务日期，查询失败或不存在返回null
     */
    public LocalDate getBusinessDate(String orderType, String businessId) {
        if (businessId == null || orderType == null) {
            log.warn("getBusinessDate: 参数为空, orderType={}, businessId={}", orderType, businessId);
            return null;
        }

        String dateStr = null;
        switch (orderType) {
            case "REGISTER":
                dateStr = getRegisterVisitDate(businessId);
                break;
            case "MEDICAL":
                dateStr = getMedicalOrderCreateDate(businessId);
                break;
            case "PRESCRIPTION":
                dateStr = getPrescriptionCreateDate(businessId);
                break;
            default:
                log.warn("getBusinessDate: 未知订单类型, orderType={}", orderType);
                return null;
        }

        if (dateStr == null) {
            return null;
        }

        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("getBusinessDate: 日期解析失败, orderType={}, businessId={}, dateStr={}",
                    orderType, businessId, dateStr);
            return null;
        }
    }

    /**
     * 查询挂号的visit_date，格式: yyyy-MM-dd
     */
    private String getRegisterVisitDate(String registerId) {
        try {
            Result<String> result = patientServiceFeignClient.getRegisterVisitDate(registerId);
            if (result != null && result.getCode() == 200) {
                return result.getData();
            }
            log.warn("查询挂号visit_date失败: registerId={}, result={}", registerId, result);
            return null;
        } catch (Exception e) {
            log.error("查询挂号visit_date异常: registerId={}", registerId, e);
            return null;
        }
    }

    /**
     * 查询医疗订单的create_time，格式: yyyy-MM-dd
     */
    private String getMedicalOrderCreateDate(String orderId) {
        try {
            Result<String> result = patientServiceFeignClient.getMedicalOrderCreateDate(orderId);
            if (result != null && result.getCode() == 200) {
                return result.getData();
            }
            log.warn("查询医疗订单create_time失败: orderId={}, result={}", orderId, result);
            return null;
        } catch (Exception e) {
            log.error("查询医疗订单create_time异常: orderId={}", orderId, e);
            return null;
        }
    }

    /**
     * 查询处方的create_time，格式: yyyy-MM-dd
     */
    private String getPrescriptionCreateDate(String prescriptionId) {
        try {
            Result<String> result = patientServiceFeignClient.getPrescriptionCreateDate(prescriptionId);
            if (result != null && result.getCode() == 200) {
                return result.getData();
            }
            log.warn("查询处方create_time失败: prescriptionId={}, result={}", prescriptionId, result);
            return null;
        } catch (Exception e) {
            log.error("查询处方create_time异常: prescriptionId={}", prescriptionId, e);
            return null;
        }
    }
}
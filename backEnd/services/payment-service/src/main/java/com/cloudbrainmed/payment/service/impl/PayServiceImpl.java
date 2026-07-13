package com.cloudbrainmed.payment.service.impl;

import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.result.PageResult;
import com.cloudbrainmed.payment.dto.PayQueryDto;
import com.cloudbrainmed.payment.dto.UnifiedPayDto;
import com.cloudbrainmed.payment.entity.Pay;
import com.cloudbrainmed.payment.mapper.PayMapper;
import com.cloudbrainmed.payment.service.BusinessStatusService;
import com.cloudbrainmed.payment.service.MedicalOrderCallbackService;
import com.cloudbrainmed.payment.service.PayService;
import com.cloudbrainmed.payment.vo.PayResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PayServiceImpl implements PayService {

    private static final Logger log = LoggerFactory.getLogger(PayServiceImpl.class);

    private final PayMapper payMapper;
    private final MedicalOrderCallbackService medicalOrderCallbackService;
    private final BusinessStatusService businessStatusService;

    public PayServiceImpl(
            PayMapper payMapper,
            MedicalOrderCallbackService medicalOrderCallbackService,
            BusinessStatusService businessStatusService) {
        this.payMapper = payMapper;
        this.medicalOrderCallbackService = medicalOrderCallbackService;
        this.businessStatusService = businessStatusService;
    }

    // ==================== 原有方法 ====================

    @Override
    @Transactional
    public PayResultVo createPayOrder(UnifiedPayDto dto) {
        // 检查是否已存在未支付的订单
        Pay existingPay = payMapper.selectByBusinessId(dto.getBusinessId(), dto.getOrderType());

        if (existingPay != null) {
            // 如果存在待支付订单，直接返回该订单
            if ("WAITING".equals(existingPay.getPayStatus())) {
                throw new BusinessException("该业务已存在待支付订单，请勿重复创建");
            }
            // 如果已经支付成功，不允许再次创建
            if ("PAID".equals(existingPay.getPayStatus())) {
                throw new BusinessException("该业务已完成支付，请勿重复支付");
            }
            // 如果是已取消或已退款，允许创建新订单
            if ("CANCELLED".equals(existingPay.getPayStatus()) || "REFUNDED".equals(existingPay.getPayStatus())) {
                throw new BusinessException("该业务支付已取消/退款，请重新发起预约");
            }
        }

        // 生成支付ID
        String payId = generatePayId();

        Pay pay = new Pay();
        pay.setPayId(payId);
        pay.setOrderType(dto.getOrderType());
        pay.setBusinessId(dto.getBusinessId());
        pay.setPatientId(dto.getPatientId());
        pay.setPatientName(dto.getPatientName());
        pay.setTotalAmount(dto.getAmount());
        pay.setPayStatus("WAITING");
        pay.setPayTime(LocalDateTime.now());

        payMapper.insert(pay);

        return convertToVo(pay);
    }

    @Override
    @Transactional
    public boolean paySuccess(String payId) {
        Pay pay = payMapper.selectByPayId(payId);
        if (pay == null) {
            throw new BusinessException("支付订单不存在");
        }
        if ("PAID".equals(pay.getPayStatus())) {
            return true;
        }
        if (!"WAITING".equals(pay.getPayStatus())) {
            throw new BusinessException("支付订单状态异常");
        }

        // 支付成功时，更新 payTime 为支付完成时间
        int result = payMapper.updatePaySuccess(payId, "PAID", LocalDateTime.now());
        if (result != 1) {
            throw new BusinessException("支付状态更新失败");
        }
        if ("MEDICAL".equals(pay.getOrderType())) {
            notifyMedicalOrderPaid(pay);
        } else {
            syncBusinessPayStatus(pay, "PAID");
        }
        return result > 0;
    }

    @Override
    @Transactional
    public boolean cancelPay(String payId) {
        Pay pay = payMapper.selectByPayId(payId);
        if (pay == null) {
            throw new BusinessException("支付订单不存在");
        }
        if (!"WAITING".equals(pay.getPayStatus())) {
            throw new BusinessException("支付订单状态异常");
        }

        int result = payMapper.updatePayStatusFrom(payId, "CANCELLED", "WAITING");
        if (result != 1) {
            throw new BusinessException("支付状态更新失败");
        }
        syncBusinessPayStatus(pay, "CANCELLED");
        return result > 0;
    }

    @Override
    @Transactional
    public boolean refundPay(String payId) {
        Pay pay = payMapper.selectByPayId(payId);
        if (pay == null) {
            throw new BusinessException("支付订单不存在");
        }
        if (!"PAID".equals(pay.getPayStatus())) {
            throw new BusinessException("只有已支付的订单才能退款");
        }

        int result = payMapper.updatePayStatusFrom(payId, "REFUNDED", "PAID");
        if (result != 1) {
            throw new BusinessException("支付状态更新失败");
        }
        syncBusinessPayStatus(pay, "REFUNDED");
        return result > 0;
    }

    @Override
    public boolean canRefund(String payId) {
        Pay pay = payMapper.selectByPayId(payId);
        if (pay == null) {
            log.warn("canRefund: 支付订单不存在, payId={}", payId);
            return false;
        }

        // 1. 支付状态必须是 PAID
        if (!"PAID".equals(pay.getPayStatus())) {
            log.info("canRefund: 支付状态不是PAID, payId={}, payStatus={}", payId, pay.getPayStatus());
            return false;
        }

        // 2. 查询业务表的支付状态
        String businessPayStatus = businessStatusService.getPayStatus(
                pay.getOrderType(), pay.getBusinessId());

        // 3. 业务支付状态必须是 PAID
        if (!"PAID".equals(businessPayStatus)) {
            log.info("canRefund: 业务状态不是PAID, payId={}, businessPayStatus={}",
                    payId, businessPayStatus);
            return false;
        }

        // 4. 查询业务日期，判断是否超过退款截止时间
        //    - REGISTER: visit_date + 1天
        //    - MEDICAL: create_time + 1天
        //    - PRESCRIPTION: create_time + 1天
        LocalDate businessDate = businessStatusService.getBusinessDate(
                pay.getOrderType(), pay.getBusinessId());

        if (businessDate == null) {
            // 查不到业务日期，保守处理：不允许退款
            log.warn("canRefund: 查不到业务日期，不允许退款, payId={}, orderType={}, businessId={}",
                    payId, pay.getOrderType(), pay.getBusinessId());
            return false;
        }

        // 退款截止时间 = 业务日期 + 1天
        // 例如：businessDate = 2026-07-13
        // 退款截止时间 = 2026-07-15 00:00:00（即7月14日全天结束）
        // 患者在 2026-07-14 23:59:59 之前都可以申请退款
        LocalDate refundDeadlineDate = businessDate.plusDays(1);
        LocalDate today = LocalDate.now();

        if (today.isAfter(refundDeadlineDate)) {
            log.info("canRefund: 已超过退款截止时间, payId={}, businessDate={}, refundDeadlineDate={}, today={}",
                    payId, businessDate, refundDeadlineDate, today);
            return false;
        }

        log.info("canRefund: 允许退款, payId={}, businessDate={}, refundDeadlineDate={}, today={}",
                payId, businessDate, refundDeadlineDate, today);
        return true;
    }

    private void syncBusinessPayStatus(Pay pay, String payStatus) {
        if (pay.getBusinessId() == null || pay.getOrderType() == null) {
            return;
        }
        int updated = payMapper.updateBusinessPayStatus(
                pay.getOrderType(), pay.getBusinessId(), payStatus);
        if (updated != 1) {
            throw new BusinessException("业务支付状态同步失败");
        }
    }

    private void notifyMedicalOrderPaid(Pay pay) {
        if (!"MEDICAL".equals(pay.getOrderType()) || pay.getBusinessId() == null) {
            return;
        }
        if (!medicalOrderCallbackService.onOrderPaid(pay.getBusinessId())) {
            throw new BusinessException("medical order callback failed");
        }
    }

    // ==================== 查询方法 ====================

    @Override
    public PayResultVo getByPayId(String payId) {
        Pay pay = payMapper.selectByPayId(payId);
        if (pay == null) {
            return null;
        }
        return convertToVo(pay);
    }

    @Override
    public PayResultVo getByBusinessId(String businessId, String orderType) {
        Pay pay = payMapper.selectByBusinessId(businessId, orderType);
        if (pay == null) {
            return null;
        }
        return convertToVo(pay);
    }

    @Override
    public List<PayResultVo> getPayHistory(String patientId) {
        List<Pay> payList = payMapper.selectByPatientId(patientId);
        List<PayResultVo> result = new ArrayList<>();
        for (Pay pay : payList) {
            result.add(convertToVo(pay));
        }
        return result;
    }

    @Override
    public PageResult<PayResultVo> queryPayPage(PayQueryDto dto) {
        int offset = (dto.getPageNum() - 1) * dto.getPageSize();
        List<Pay> payList = payMapper.selectPage(
                dto.getPatientId(),
                dto.getPayStatus(),
                dto.getOrderType(),
                offset,
                dto.getPageSize()
        );

        Long total = payMapper.countByPatientId(
                dto.getPatientId(),
                dto.getPayStatus(),
                dto.getOrderType()
        );

        List<PayResultVo> records = new ArrayList<>();
        for (Pay pay : payList) {
            records.add(convertToVo(pay));
        }

        return new PageResult<>(records, total, dto.getPageNum(), dto.getPageSize());
    }

    // ==================== 增强版查询（新增） ====================

    @Override
    public List<PayResultVo> getPayHistoryWithStatus(String patientId) {
        log.info("getPayHistoryWithStatus: patientId={}", patientId);

        List<Pay> payList = payMapper.selectByPatientId(patientId);
        log.info("查询到 {} 条支付记录", payList.size());

        List<PayResultVo> result = new ArrayList<>();

        for (Pay pay : payList) {
            PayResultVo vo = convertToVo(pay);
            String payStatus = pay.getPayStatus();

            // 处理待支付订单的过期判断
            if ("WAITING".equals(payStatus)) {
                boolean isExpired = checkOrderExpired(pay);
                vo.setIsExpired(isExpired);
                log.info("待支付订单: payId={}, payTime={}, isExpired={}",
                        pay.getPayId(), pay.getPayTime(), isExpired);

                if (isExpired) {
                    autoCancelExpiredOrder(pay);
                    vo.setPayStatus("CANCELLED");
                    vo.setPayStatusText("已取消（过期）");
                    log.info("自动取消过期订单: payId={}", pay.getPayId());
                }
            }

            // 处理已支付订单：查询业务状态，判断是否可退款
            if ("PAID".equals(payStatus)) {
                String businessPayStatus = businessStatusService.getPayStatus(
                        pay.getOrderType(), pay.getBusinessId());
                vo.setBusinessStatus(businessPayStatus);

                // 统一调用 canRefund 方法判断退款资格（包含业务日期+1天的时间校验）
                boolean canRefund = canRefund(pay.getPayId());
                vo.setCanRefund(canRefund);

                log.info("已支付订单: payId={}, businessPayStatus={}, canRefund={}",
                        pay.getPayId(), businessPayStatus, canRefund);
            }

            result.add(vo);
        }

        log.info("getPayHistoryWithStatus 返回 {} 条记录", result.size());
        return result;
    }

    // ==================== 私有方法 ====================

    /**
     * 检查订单是否过期（创建后超过24小时未支付）
     */
    private boolean checkOrderExpired(Pay pay) {
        LocalDateTime payTime = pay.getPayTime();
        if (payTime == null) {
            log.warn("payTime is null for payId: {}", pay.getPayId());
            return false;
        }
        LocalDateTime expireTime = payTime.plusHours(24);
        boolean expired = LocalDateTime.now().isAfter(expireTime);
        log.info("订单过期检查: payId={}, payTime={}, expireTime={}, now={}, expired={}",
                pay.getPayId(), payTime, expireTime, LocalDateTime.now(), expired);
        return expired;
    }

    /**
     * 自动取消过期订单
     */
    @Transactional
    protected void autoCancelExpiredOrder(Pay pay) {
        // 1. 先更新 pay 表状态
        int result = payMapper.updatePayStatusFrom(pay.getPayId(), "CANCELLED", "WAITING");
        if (result == 1) {
            log.info("pay表更新成功: payId={}", pay.getPayId());
            // 2. 尝试同步业务表（失败不影响主流程）
            try {
                syncBusinessPayStatus(pay, "CANCELLED");
                log.info("业务表同步成功: payId={}, businessId={}", pay.getPayId(), pay.getBusinessId());
            } catch (Exception e) {
                // 业务表同步失败，只记录日志，不抛出异常
                log.warn("业务表同步失败(不影响主流程): payId={}, businessId={}, error={}",
                        pay.getPayId(), pay.getBusinessId(), e.getMessage());
            }
        } else {
            log.warn("pay表更新失败: payId={}, 可能已被其他操作修改", pay.getPayId());
        }
    }

    /**
     * 根据就诊日期判断是否可退款
     * 规则：当前时间 <= visit_date + 1天，则可以退款
     */
    private boolean checkCanRefundByVisitDate(LocalDateTime visitDate) {
        if (visitDate == null) {
            // 非挂号订单，默认允许退款
            return true;
        }
        LocalDateTime deadline = visitDate.plusDays(1).toLocalDate().plusDays(1).atStartOfDay();
        return LocalDateTime.now().isBefore(deadline);
    }

    /**
     * 实体转VO
     */
    private PayResultVo convertToVo(Pay pay) {
        if (pay == null) {
            return null;
        }
        PayResultVo vo = new PayResultVo();
        vo.setPayId(pay.getPayId());
        vo.setOrderType(pay.getOrderType());
        vo.setBusinessId(pay.getBusinessId());
        vo.setPatientId(pay.getPatientId());
        vo.setPatientName(pay.getPatientName());
        vo.setTotalAmount(pay.getTotalAmount());
        vo.setPayStatus(pay.getPayStatus());
        vo.setPayStatusText(getPayStatusText(pay.getPayStatus()));
        vo.setPayTime(pay.getPayTime());
        vo.setDescription(getOrderTypeDesc(pay.getOrderType()));
        return vo;
    }

    private String getPayStatusText(String status) {
        switch (status) {
            case "WAITING": return "待支付";
            case "PAID": return "已支付";
            case "CANCELLED": return "已取消";
            case "REFUNDED": return "已退款";
            default: return status;
        }
    }

    private String getOrderTypeDesc(String orderType) {
        switch (orderType) {
            case "REGISTER": return "挂号费";
            case "MEDICAL": return "医技检查费";
            case "PRESCRIPTION": return "药品费";
            default: return "其他费用";
        }
    }

    /**
     * 生成支付ID：pay001, pay002, pay003...
     */
    private String generatePayId() {
        String maxPayId = payMapper.selectMaxPayId();

        int nextNumber = 1;
        if (maxPayId != null && maxPayId.startsWith("pay")) {
            try {
                String numStr = maxPayId.substring(3);
                nextNumber = Integer.parseInt(numStr) + 1;
            } catch (NumberFormatException e) {
                nextNumber = 1;
            }
        }

        return String.format("pay%03d", nextNumber);
    }
}
package com.cloudbrainmed.payment.service.impl;

import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.payment.dto.PayQueryDto;
import com.cloudbrainmed.payment.dto.UnifiedPayDto;
import com.cloudbrainmed.payment.entity.Pay;
import com.cloudbrainmed.payment.mapper.PayMapper;
import com.cloudbrainmed.payment.service.MedicalOrderCallbackService;
import com.cloudbrainmed.payment.service.PayService;
import com.cloudbrainmed.payment.vo.PayResultVo;
import com.cloudbrainmed.common.result.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PayServiceImpl implements PayService {

    private final PayMapper payMapper;
    private final MedicalOrderCallbackService medicalOrderCallbackService;

    public PayServiceImpl(
            PayMapper payMapper,
            MedicalOrderCallbackService medicalOrderCallbackService) {
        this.payMapper = payMapper;
        this.medicalOrderCallbackService = medicalOrderCallbackService;
    }

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

        // ✅ 支付成功时，更新 payTime 为支付完成时间
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

        // 使用全参构造函数
        return new PageResult<>(records, total, dto.getPageNum(), dto.getPageSize());
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
        vo.setPayTime(pay.getPayTime());  // ✅ 统一使用 payTime
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
        // 查询当前最大序号
        String maxPayId = payMapper.selectMaxPayId();

        int nextNumber = 1;
        if (maxPayId != null && maxPayId.startsWith("pay")) {
            try {
                // 提取数字部分：pay001 -> 1, pay999 -> 999
                String numStr = maxPayId.substring(3);
                nextNumber = Integer.parseInt(numStr) + 1;
            } catch (NumberFormatException e) {
                // 如果解析失败，从1开始
                nextNumber = 1;
            }
        }

        // 格式化为3位数字：1 -> 001, 10 -> 010, 100 -> 100
        return String.format("pay%03d", nextNumber);
    }
}

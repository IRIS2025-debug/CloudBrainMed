package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.api.feign.PaymentFeignClient;
import com.cloudbrainmed.common.constant.MedicalItemCodeEnum;
import com.cloudbrainmed.common.constant.UrgencyLevelEnum;
import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.doctor.dto.MedicalOrderConfirmRequest;
import com.cloudbrainmed.doctor.dto.MedicalOrderConfirmResponse;
import com.cloudbrainmed.doctor.dto.MedicalOrderItemRequest;
import com.cloudbrainmed.doctor.entity.ConsultRecord;
import com.cloudbrainmed.doctor.entity.MedicalItem;
import com.cloudbrainmed.doctor.entity.MedicalOrder;
import com.cloudbrainmed.doctor.entity.MedicalOrderItem;
import com.cloudbrainmed.doctor.mapper.ConsultMapper;
import com.cloudbrainmed.doctor.mapper.MedicalItemMapper;
import com.cloudbrainmed.doctor.mapper.MedicalOrderMapper;
import com.cloudbrainmed.doctor.service.MedicalOrderService;
import com.cloudbrainmed.doctor.vo.InspectionOrderVo;
import com.cloudbrainmed.payment.dto.UnifiedPayDto;
import com.cloudbrainmed.payment.vo.PayResultVo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class MedicalOrderServiceImpl implements MedicalOrderService {

    private final ConsultMapper consultMapper;
    private final MedicalItemMapper medicalItemMapper;
    private final MedicalOrderMapper medicalOrderMapper;
    private final PaymentFeignClient paymentFeignClient;
    private final ObjectMapper objectMapper;

    public MedicalOrderServiceImpl(
            ConsultMapper consultMapper,
            MedicalItemMapper medicalItemMapper,
            MedicalOrderMapper medicalOrderMapper,
            PaymentFeignClient paymentFeignClient,
            ObjectMapper objectMapper) {
        this.consultMapper = consultMapper;
        this.medicalItemMapper = medicalItemMapper;
        this.medicalOrderMapper = medicalOrderMapper;
        this.paymentFeignClient = paymentFeignClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<InspectionOrderVo> getAllLabOrders() {
        return medicalOrderMapper.selectAllLabOrders();
    }

    @Override
    public MedicalOrder getByOrderId(String orderId) {
        return medicalOrderMapper.selectByOrderId(orderId);
    }
/*
    @Override
    @Transactional
    public MedicalOrderConfirmResponse confirm(
            MedicalOrderConfirmRequest request, String doctorId) {
        ConsultRecord consult = requireOwnedConsult(
                request.getRegisterId(), doctorId);
        String requestedOrderUrgency =
                parseUrgency(request.getUrgencyLevel());
        List<ResolvedItem> resolvedItems = resolveItems(
                request.getItems(), requestedOrderUrgency);
        String orderUrgency = highestUrgency(
                requestedOrderUrgency, resolvedItems);
        LocalDateTime now = LocalDateTime.now();
        String orderId = newId("MO", 30);
        boolean aiAssisted = hasText(request.getAiTraceId());
        String aiTraceId = aiAssisted
                ? request.getAiTraceId().trim() : null;
        if (aiAssisted) {
            validateAiRecommendation(
                    aiTraceId, consult, resolvedItems);
        }

        MedicalOrder order = new MedicalOrder();
        order.setOrderId(orderId);
        order.setPatientId(consult.getPatientId());
        order.setRegisterId(consult.getRegisterId());
        order.setDoctorId(doctorId);
        order.setClinicalSummary(request.getClinicalSummary().trim());
        order.setUrgencyLevel(orderUrgency);
        order.setSourceType(aiAssisted ? "AI_ASSISTED" : "MANUAL");
        order.setAiTraceId(aiTraceId);
        order.setStatus("WAITING_ASSIGN");
        order.setPayStatus("WAITING");
        order.setConfirmedTime(now);
        order.setCreateTime(now);
        medicalOrderMapper.insertOrder(order);

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ResolvedItem resolved : resolvedItems) {
            MedicalItem source = resolved.item();
            MedicalOrderItem orderItem = new MedicalOrderItem();
            orderItem.setOrderItemId(newId("MOI", 29));
            orderItem.setOrderId(orderId);
            orderItem.setItemId(source.getItemId());
            orderItem.setItemCode(source.getItemCode());
            orderItem.setItemName(source.getItemName());
            orderItem.setItemCategory(source.getItemCategory());
            orderItem.setAssignedDeptId(source.getDeptId());
            orderItem.setUrgencyLevel(resolved.urgencyLevel());
            orderItem.setPrice(source.getPrice() == null
                    ? BigDecimal.ZERO : source.getPrice());
            orderItem.setStatus("WAITING_ASSIGN");
            orderItem.setCreateTime(now);
            medicalOrderMapper.insertOrderItem(orderItem);
            totalAmount = totalAmount.add(orderItem.getPrice());
        }

        if (medicalOrderMapper.keepConsultInProgress(
                consult.getRegisterId(), doctorId) != 1) {
            throw new BusinessException("更新接诊状态失败");
        }
        return new MedicalOrderConfirmResponse(
                orderId,
                order.getSourceType(),
                resolvedItems.size(),
                totalAmount);
    }

    private ConsultRecord requireOwnedConsult(
            String registerId, String doctorId) {
        ConsultRecord consult = consultMapper.findDetail(registerId);
        if (consult == null) {
            throw new BusinessException("就诊记录不存在");
        }
        if (!hasText(doctorId) || !doctorId.equals(consult.getDoctorId())) {
            throw new BusinessException("无权为该患者开具检查检验申请");
        }
        if ("COMPLETED".equals(consult.getConsultStatus())) {
            throw new BusinessException("接诊已完成，不能继续开具检查检验申请");
        }
        return consult;
    }
*/
    @Override
    @Transactional
    public MedicalOrderConfirmResponse confirm(
            MedicalOrderConfirmRequest request, String doctorId) {
        ConsultRecord consult = requireOwnedConsult(
                request.getRegisterId(), doctorId);
        String requestedOrderUrgency =
                parseUrgency(request.getUrgencyLevel());
        List<ResolvedItem> resolvedItems = resolveItems(
                request.getItems(), requestedOrderUrgency);
        String orderUrgency = highestUrgency(
                requestedOrderUrgency, resolvedItems);
        LocalDateTime now = LocalDateTime.now();
        String orderId = newId("MO", 30);
        boolean aiAssisted = hasText(request.getAiTraceId());
        String aiTraceId = aiAssisted
                ? request.getAiTraceId().trim() : null;
        if (aiAssisted) {
            validateAiRecommendation(
                    aiTraceId, consult, resolvedItems);
        }

        MedicalOrder order = new MedicalOrder();
        order.setOrderId(orderId);
        order.setPatientId(consult.getPatientId());
        order.setRegisterId(consult.getRegisterId());
        order.setDoctorId(doctorId);
        order.setClinicalSummary(request.getClinicalSummary().trim());
        order.setUrgencyLevel(orderUrgency);
        order.setSourceType(aiAssisted ? "AI_ASSISTED" : "MANUAL");
        order.setAiTraceId(aiTraceId);
        order.setStatus("WAITING_ASSIGN");
        order.setPayStatus("WAITING");
        order.setConfirmedTime(now);
        order.setCreateTime(now);
        medicalOrderMapper.insertOrder(order);

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ResolvedItem resolved : resolvedItems) {
            MedicalItem source = resolved.item();
            MedicalOrderItem orderItem = new MedicalOrderItem();
            orderItem.setOrderItemId(newId("MOI", 29));
            orderItem.setOrderId(orderId);
            orderItem.setItemId(source.getItemId());
            orderItem.setItemCode(source.getItemCode());
            orderItem.setItemName(source.getItemName());
            orderItem.setItemCategory(source.getItemCategory());
            orderItem.setAssignedDeptId(source.getDeptId());
            orderItem.setUrgencyLevel(resolved.urgencyLevel());
            orderItem.setPrice(source.getPrice() == null
                    ? BigDecimal.ZERO : source.getPrice());
            orderItem.setStatus("WAITING_ASSIGN");
            orderItem.setCreateTime(now);
            medicalOrderMapper.insertOrderItem(orderItem);
            totalAmount = totalAmount.add(orderItem.getPrice());
        }

        if (medicalOrderMapper.keepConsultInProgress(
                consult.getRegisterId(), doctorId) != 1) {
            throw new BusinessException("Failed to update consult status");
        }
        createPayOrder(order, consult, totalAmount);
        return new MedicalOrderConfirmResponse(
                orderId,
                order.getSourceType(),
                resolvedItems.size(),
                totalAmount);
    }

    @Override
    @Transactional
    public MedicalOrder assignOrder(String orderId, String assignedRoom) {
        MedicalOrder order = medicalOrderMapper.selectByOrderId(orderId);
        if (order == null) {
            throw new BusinessException("medical order not found");
        }
        if (!"PAID".equals(order.getPayStatus())) {
            throw new BusinessException("medical order pay status is not PAID");
        }
        if (!"WAITING_ASSIGN".equals(order.getStatus())) {
            throw new BusinessException("medical order is not waiting assignment");
        }
        if (!hasText(assignedRoom)) {
            throw new BusinessException("assigned room is required");
        }
        String room = assignedRoom.trim();
        if (medicalOrderMapper.assignOrder(orderId, room) != 1) {
            throw new BusinessException("assign medical order failed");
        }
        order.setStatus("QUEUED");
        order.setAssignedRoom(room);
        return order;
    }

    private void createPayOrder(
            MedicalOrder order, ConsultRecord consult, BigDecimal totalAmount) {
        UnifiedPayDto dto = new UnifiedPayDto();
        dto.setPatientId(order.getPatientId());
        dto.setPatientName(patientName(consult));
        dto.setOrderType("MEDICAL");
        dto.setBusinessId(order.getOrderId());
        dto.setDescription("医技检查检验费");
        dto.setAmount(totalAmount == null ? BigDecimal.ZERO : totalAmount);
        Result<PayResultVo> result = paymentFeignClient.createPayOrder(dto);
        if (result == null || result.getCode() == null || result.getCode() != 200) {
            throw new BusinessException("create medical pay order failed");
        }
    }

    private String patientName(ConsultRecord consult) {
        if (hasText(consult.getPatientName())) {
            return consult.getPatientName();
        }
        return consult.getName();
    }

    private ConsultRecord requireOwnedConsult(
            String registerId, String doctorId) {
        ConsultRecord consult = consultMapper.findDetail(registerId);
        if (consult == null) {
            throw new BusinessException("Consult record not found");
        }
        if (!hasText(doctorId) || !doctorId.equals(consult.getDoctorId())) {
            throw new BusinessException("无权 create medical order");
        }
        if ("COMPLETED".equals(consult.getConsultStatus())) {
            throw new BusinessException("Consult already completed");
        }
        return consult;
    }

    private void validateAiRecommendation(
            String traceId,
            ConsultRecord consult,
            List<ResolvedItem> resolvedItems) {
        String inputSummary = medicalOrderMapper.findAiRecommendationInput(
                traceId, consult.getPatientId());
        String outputSummary = medicalOrderMapper.findAiRecommendationOutput(
                traceId, consult.getPatientId());
        if (!hasText(inputSummary) || !hasText(outputSummary)) {
            throw new BusinessException("AI检查检验推荐记录不存在或已失效");
        }
        try {
            JsonNode input = objectMapper.readTree(inputSummary);
            if (!consult.getRegisterId().equals(
                    input.path("registerId").asText())) {
                throw new BusinessException("AI推荐与当前接诊记录不匹配");
            }

            Set<String> recommendedCodes = new HashSet<>();
            JsonNode recommendations = objectMapper.readTree(outputSummary)
                    .path("recommendations");
            if (recommendations.isArray()) {
                recommendations.forEach(item -> {
                    String code = item.path("itemCode").asText("")
                            .trim().toUpperCase(Locale.ROOT);
                    if (hasText(code)) {
                        recommendedCodes.add(code);
                    }
                });
            }
            boolean containsUnrecommended = resolvedItems.stream()
                    .map(item -> item.item().getItemCode())
                    .anyMatch(code -> !recommendedCodes.contains(code));
            if (containsUnrecommended) {
                throw new BusinessException(
                        "提交项目与AI推荐结果不一致，请重新生成或按手工申请提交");
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("AI检查检验推荐记录格式错误");
        }
    }
    private List<ResolvedItem> resolveItems(
            List<MedicalOrderItemRequest> requests,
            String orderUrgency) {
        List<ResolvedItem> result = new ArrayList<>();
        Set<String> seenCodes = new HashSet<>();
        for (MedicalOrderItemRequest request : requests) {
            String itemCode = request.getItemCode().trim()
                    .toUpperCase(Locale.ROOT);
            if (!MedicalItemCodeEnum.isSupported(itemCode)) {
                throw new BusinessException(
                        "不支持的检查检验项目编码：" + itemCode);
            }
            if (!seenCodes.add(itemCode)) {
                throw new BusinessException(
                        "检查检验项目不能重复：" + itemCode);
            }

            MedicalItem item =
                    medicalItemMapper.selectEnabledByCode(itemCode);
            if (item == null) {
                throw new BusinessException(
                        "检查检验项目不存在或已停用：" + itemCode);
            }
            MedicalItemCodeEnum codeEnum =
                    MedicalItemCodeEnum.valueOf(itemCode);
            if (!codeEnum.getCategory().name()
                    .equals(item.getItemCategory())) {
                throw new BusinessException(
                        "检查检验项目分类配置错误：" + itemCode);
            }

            String itemUrgency = hasText(request.getUrgencyLevel())
                    ? parseUrgency(request.getUrgencyLevel())
                    : orderUrgency;
            result.add(new ResolvedItem(item, itemUrgency));
        }
        return result;
    }

    private String parseUrgency(String value) {
        try {
            return UrgencyLevelEnum.from(value).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(exception.getMessage());
        }
    }

    private String highestUrgency(
            String orderUrgency, List<ResolvedItem> items) {
        UrgencyLevelEnum highest =
                UrgencyLevelEnum.valueOf(orderUrgency);
        for (ResolvedItem item : items) {
            UrgencyLevelEnum itemUrgency =
                    UrgencyLevelEnum.valueOf(item.urgencyLevel());
            if (itemUrgency.ordinal() > highest.ordinal()) {
                highest = itemUrgency;
            }
        }
        return highest.name();
    }

    private String newId(String prefix, int randomLength) {
        return prefix + UUID.randomUUID().toString()
                .replace("-", "").substring(0, randomLength);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record ResolvedItem(
            MedicalItem item, String urgencyLevel) {
    }
}

// src/main/java/com/cloudbrainmed/patient/service/impl/MedicalOrderServiceImpl.java
package com.cloudbrainmed.patient.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudbrainmed.patient.entity.*;
import com.cloudbrainmed.patient.mapper.*;
import com.cloudbrainmed.patient.service.MedicalOrderService;
import com.cloudbrainmed.patient.vo.IndicatorVo;
import com.cloudbrainmed.patient.vo.MedicalOrderGroupVo;
import com.cloudbrainmed.patient.vo.MedicalOrderItemVo;
import com.cloudbrainmed.patient.vo.ReportDetailVo;
import com.cloudbrainmed.patient.vo.RoomInfoVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MedicalOrderServiceImpl implements MedicalOrderService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final Map<String, String> STATUS_TEXT = new HashMap<>();
    private static final Map<String, String> ABNORMAL_TEXT = new HashMap<>();
    private static final Map<String, String> URGENCY_TEXT = new HashMap<>();

    static {
        STATUS_TEXT.put("WAITING_ASSIGN", "待分配");
        STATUS_TEXT.put("QUEUED", "已排队");
        STATUS_TEXT.put("IN_PROGRESS", "执行中");
        STATUS_TEXT.put("COMPLETED", "已完成");
        STATUS_TEXT.put("CANCELLED", "已取消");
        STATUS_TEXT.put("PENDING_CONFIRM", "待确认");
        STATUS_TEXT.put("DRAFT", "草稿");
        STATUS_TEXT.put("REVIEWING", "审核中");
        STATUS_TEXT.put("PUBLISHED", "已发布");
        STATUS_TEXT.put("REVOKED", "已撤回");

        ABNORMAL_TEXT.put("NORMAL", "正常");
        ABNORMAL_TEXT.put("HIGH", "偏高");
        ABNORMAL_TEXT.put("LOW", "偏低");
        ABNORMAL_TEXT.put("ABNORMAL", "异常");
        ABNORMAL_TEXT.put("CRITICAL", "危急");

        URGENCY_TEXT.put("NORMAL", "常规");
        URGENCY_TEXT.put("URGENT", "加急");
        URGENCY_TEXT.put("EMERGENCY", "紧急");
    }

    @Autowired private MedicalOrderMapper medicalOrderMapper;
    @Autowired private MedicalOrderItemMapper medicalOrderItemMapper;
    @Autowired private MedicalReportMapper medicalReportMapper;
    @Autowired private MedicalReportIndicatorMapper medicalReportIndicatorMapper;
    @Autowired private RegistrationMapper registrationMapper;
    @Autowired private PatientMapper patientMapper;
    @Autowired private DoctorMapper doctorMapper;

    @Override
    public List<MedicalOrderGroupVo> getGroupedOrdersByPatient(String patientId) { /* unchanged */
        List<MedicalOrderItem> orderItems = medicalOrderItemMapper.selectByPatientId(patientId);
        if (CollectionUtils.isEmpty(orderItems)) return new ArrayList<>();
        Set<String> orderIds = orderItems.stream().map(MedicalOrderItem::getOrderId).collect(Collectors.toSet());
        List<MedicalOrder> orders = medicalOrderMapper.selectBatchIds(new ArrayList<>(orderIds));
        Map<String, MedicalOrder> orderMap = orders.stream().collect(Collectors.toMap(MedicalOrder::getOrderId, o -> o, (v1, v2) -> v1));
        Set<String> registerIds = orders.stream().map(MedicalOrder::getRegisterId).filter(StringUtils::hasText).collect(Collectors.toSet());
        Map<String, Registration> registerMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(registerIds)) { registerMap = registrationMapper.selectBatchIds(new ArrayList<>(registerIds)).stream().collect(Collectors.toMap(Registration::getRegisterId, r -> r, (v1, v2) -> v1)); }
        Set<String> doctorIds = new HashSet<>();
        for (Registration reg : registerMap.values()) if (StringUtils.hasText(reg.getDoctorId())) doctorIds.add(reg.getDoctorId());
        for (MedicalOrder order : orders) if (StringUtils.hasText(order.getDoctorId())) doctorIds.add(order.getDoctorId());
        Map<String, Doctor> doctorMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(doctorIds)) doctorMap = doctorMapper.selectBatchIds(new ArrayList<>(doctorIds)).stream().collect(Collectors.toMap(Doctor::getDoctorId, d -> d, (v1, v2) -> v1));
        Set<String> orderItemIds = orderItems.stream().map(MedicalOrderItem::getOrderItemId).collect(Collectors.toSet());
        Map<String, MedicalReport> reportMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(orderItemIds)) reportMap = medicalReportMapper.selectByOrderItemIds(new ArrayList<>(orderItemIds)).stream().collect(Collectors.toMap(MedicalReport::getOrderItemId, r -> r, (v1, v2) -> v1));
        Patient patient = patientMapper.selectById(patientId);
        Map<String, List<MedicalOrderItem>> groupMap = new LinkedHashMap<>();
        for (MedicalOrderItem item : orderItems) { MedicalOrder order = orderMap.get(item.getOrderId()); if (order == null) continue; String registerId = StringUtils.hasText(order.getRegisterId()) ? order.getRegisterId() : "no_register_" + order.getOrderId(); groupMap.computeIfAbsent(registerId, k -> new ArrayList<>()).add(item); }
        List<MedicalOrderGroupVo> result = new ArrayList<>();
        for (Map.Entry<String, List<MedicalOrderItem>> entry : groupMap.entrySet()) {
            String registerId = entry.getKey(); List<MedicalOrderItem> items = entry.getValue(); MedicalOrderGroupVo group = new MedicalOrderGroupVo(); group.setRegisterId(registerId); Registration registration = registerMap.get(registerId);
            if (registration != null) { group.setPatientName(registration.getName()); if (StringUtils.hasText(registration.getDoctorId())) { Doctor doctor = doctorMap.get(registration.getDoctorId()); if (doctor != null) group.setDoctorName(doctor.getName()); } if (registration.getVisitDate() != null) group.setVisitDate(registration.getVisitDate().format(DATE_FORMATTER)); }
            if (!StringUtils.hasText(group.getPatientName()) && patient != null) group.setPatientName(patient.getName());
            if (!StringUtils.hasText(group.getDoctorName())) for (MedicalOrderItem item : items) { MedicalOrder order = orderMap.get(item.getOrderId()); if (order != null && StringUtils.hasText(order.getDoctorId())) { Doctor doctor = doctorMap.get(order.getDoctorId()); if (doctor != null) { group.setDoctorName(doctor.getName()); break; } } }
            if (!StringUtils.hasText(group.getVisitDate())) for (MedicalOrderItem item : items) { if (item.getCreateTime() != null) { group.setVisitDate(item.getCreateTime().format(DATE_FORMATTER)); break; } }
            BigDecimal totalAmount = BigDecimal.ZERO; List<MedicalOrderItemVo> itemVos = new ArrayList<>(); for (MedicalOrderItem item : items) { MedicalOrderItemVo vo = new MedicalOrderItemVo(); BeanUtils.copyProperties(item, vo); vo.setOrderId(item.getOrderId()); vo.setRawStatus(item.getStatus()); vo.setUrgencyLevel(URGENCY_TEXT.getOrDefault(item.getUrgencyLevel(), item.getUrgencyLevel())); vo.setStatus(STATUS_TEXT.getOrDefault(item.getStatus(), item.getStatus())); enrichOrderItemRuntimeFields(item, vo); MedicalReport report = reportMap.get(item.getOrderItemId()); if (report != null) { vo.setReportId(report.getReportId()); vo.setReportStatus(STATUS_TEXT.getOrDefault(report.getStatus(), report.getStatus())); vo.setConclusion(report.getConclusion()); vo.setAbnormalFlag(ABNORMAL_TEXT.getOrDefault(report.getAbnormalFlag(), report.getAbnormalFlag())); vo.setReportTime(report.getReportTime()); vo.setFollowUpAdvice(report.getFollowUpAdvice()); } if (item.getPrice() != null) totalAmount = totalAmount.add(item.getPrice()); itemVos.add(vo); }
            itemVos.sort((a, b) -> { if (a.getCreateTime() == null) return 1; if (b.getCreateTime() == null) return -1; return b.getCreateTime().compareTo(a.getCreateTime()); }); group.setItems(itemVos); group.setItemCount(itemVos.size()); group.setTotalAmount(totalAmount); group.setStatus(calculateGroupStatus(itemVos)); result.add(group);
        }
        result.sort((a, b) -> { if (a.getVisitDate() == null) return 1; if (b.getVisitDate() == null) return -1; return b.getVisitDate().compareTo(a.getVisitDate()); }); return result;
    }

    @Override
    public ReportDetailVo getReportDetail(String orderItemId) {
        MedicalOrderItem orderItem = medicalOrderItemMapper.selectByOrderItemId(orderItemId); if (orderItem == null) throw new RuntimeException("未找到该检查记录");
        MedicalOrder order = medicalOrderMapper.selectByOrderId(orderItem.getOrderId()); if (order == null) throw new RuntimeException("未找到申请记录");
        MedicalReport report = medicalReportMapper.selectByOrderItemId(orderItemId);
        Patient patient = patientMapper.selectById(order.getPatientId());
        Registration registration = StringUtils.hasText(order.getRegisterId()) ? registrationMapper.selectById(order.getRegisterId()) : null;
        Doctor doctor = StringUtils.hasText(order.getDoctorId()) ? doctorMapper.selectById(order.getDoctorId()) : null;

        ReportDetailVo detail = new ReportDetailVo();
        BeanUtils.copyProperties(orderItem, detail);
        detail.setOrderItemId(orderItemId);
        detail.setOrderId(order.getOrderId());
        detail.setPatientId(order.getPatientId());
        detail.setUrgencyLevel(URGENCY_TEXT.getOrDefault(orderItem.getUrgencyLevel(), orderItem.getUrgencyLevel()));
        if (patient != null) detail.setPatientName(patient.getName());
        if (registration != null) { detail.setRegisterId(registration.getRegisterId()); if (!StringUtils.hasText(detail.getPatientName())) detail.setPatientName(registration.getName()); if (StringUtils.hasText(registration.getDoctorId())) { Doctor regDoctor = doctorMapper.selectById(registration.getDoctorId()); if (regDoctor != null) detail.setDoctorName(regDoctor.getName()); } }
        if (!StringUtils.hasText(detail.getDoctorName()) && doctor != null) detail.setDoctorName(doctor.getName());

        if (report != null) {
            BeanUtils.copyProperties(report, detail);
            detail.setAbnormalFlag(ABNORMAL_TEXT.getOrDefault(report.getAbnormalFlag(), report.getAbnormalFlag()));
            detail.setStatus(STATUS_TEXT.getOrDefault(report.getStatus(), report.getStatus()));
            detail.setFollowUpAdvice(report.getFollowUpAdvice());
            if ("LAB".equals(orderItem.getItemCategory())) {
                List<MedicalReportIndicator> indicators = medicalReportIndicatorMapper.selectByReportId(report.getReportId());
                if (!CollectionUtils.isEmpty(indicators)) detail.setIndicators(indicators.stream().map(ind -> { IndicatorVo vo = new IndicatorVo(); BeanUtils.copyProperties(ind, vo); vo.setAbnormalFlag(ABNORMAL_TEXT.getOrDefault(ind.getAbnormalFlag(), ind.getAbnormalFlag())); return vo; }).collect(Collectors.toList()));
            }
        }

        String rawStatus = orderItem.getStatus();
        if ("QUEUED".equals(rawStatus)) {
            Integer queueCount = medicalOrderItemMapper.countQueuedByItemId(orderItem.getItemId());
            Integer duration = medicalOrderItemMapper.selectEstimatedDurationMinByItemId(orderItem.getItemId());
            detail.setQueueCount(queueCount == null ? 0 : queueCount);
            detail.setEstimatedWaitMinutes((queueCount == null ? 0 : queueCount) * (duration == null ? 0 : duration));
        } else if ("IN_PROCESS".equals(rawStatus)) {
            List<RoomInfoVo> rooms = StringUtils.hasText(orderItem.getAssignedDoctorId()) ? medicalOrderItemMapper.selectRoomsByOperator(orderItem.getAssignedDoctorId()) : Collections.emptyList();
            if (!CollectionUtils.isEmpty(rooms)) { detail.setRoomId(rooms.get(0).getRoomId()); detail.setRoomName(rooms.get(0).getRoomName()); }
        }
        return detail;
    }

    private void enrichOrderItemRuntimeFields(MedicalOrderItem item, MedicalOrderItemVo vo) {
        if (item == null || vo == null) {
            return;
        }
        String rawStatus = item.getStatus();
        if ("QUEUED".equals(rawStatus)) {
            String itemKey = StringUtils.hasText(item.getItemId()) ? item.getItemId() : item.getItemCode();
            Integer queueCount = StringUtils.hasText(itemKey) ? medicalOrderItemMapper.countQueuedByItemId(itemKey) : 0;
            Integer duration = StringUtils.hasText(itemKey) ? medicalOrderItemMapper.selectEstimatedDurationMinByItemId(itemKey) : 0;
            vo.setQueueCount(queueCount == null ? 0 : queueCount);
            vo.setEstimatedWaitMinutes((queueCount == null ? 0 : queueCount) * (duration == null ? 0 : duration));
        } else if ("IN_PROCESS".equals(rawStatus)) {
            String operator = StringUtils.hasText(item.getAssignedDoctorId()) ? item.getAssignedDoctorId() : item.getAssignedDeptId();
            List<RoomInfoVo> rooms = StringUtils.hasText(operator) ? medicalOrderItemMapper.selectRoomsByOperator(operator) : Collections.emptyList();
            if (!CollectionUtils.isEmpty(rooms)) {
                vo.setRoomId(rooms.get(0).getRoomId());
                vo.setRoomName(rooms.get(0).getRoomName());
            }
        }
    }

    /**
     * 查询医疗订单的支付状态
     */
    @Override
    public String getPayStatus(String orderId) {
        if (orderId == null) {
            return "UNKNOWN";
        }
        MedicalOrder order = medicalOrderMapper.selectByOrderId(orderId);
        if (order == null) {
            return "UNKNOWN";
        }
        return order.getPayStatus() != null ? order.getPayStatus() : "WAITING";
    }

    @Override
    public MedicalOrder getMedicalOrder(String orderId) {
        return medicalOrderMapper.selectByOrderId(orderId);
    }

    private String calculateGroupStatus(List<MedicalOrderItemVo> items) {
        if (CollectionUtils.isEmpty(items)) return "待检查";
        boolean hasCancelled = false, hasCompleted = true, hasInProgress = false, hasWaiting = false;
        for (MedicalOrderItemVo item : items) { String status = item.getStatus(); if ("已取消".equals(status)) hasCancelled = true; else if ("已完成".equals(status)) { } else if ("执行中".equals(status)) { hasInProgress = true; hasCompleted = false; } else { hasWaiting = true; hasCompleted = false; } }
        if (hasCompleted && !hasInProgress && !hasWaiting && !hasCancelled) return "已完成";
        if (hasInProgress) return "检查中";
        if (hasWaiting) return "待检查";
        if (hasCancelled && !hasCompleted && !hasInProgress && !hasWaiting) return "已取消";
        return "待检查";
    }
}

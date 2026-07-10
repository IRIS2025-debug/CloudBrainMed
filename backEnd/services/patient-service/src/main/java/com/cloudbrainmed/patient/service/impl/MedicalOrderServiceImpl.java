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

    @Autowired
    private MedicalOrderMapper medicalOrderMapper;

    @Autowired
    private MedicalOrderItemMapper medicalOrderItemMapper;

    @Autowired
    private MedicalReportMapper medicalReportMapper;

    @Autowired
    private MedicalReportIndicatorMapper medicalReportIndicatorMapper;

    @Autowired
    private RegistrationMapper registrationMapper;

    @Autowired
    private PatientMapper patientMapper;

    @Autowired
    private DoctorMapper doctorMapper;

    @Override
    public List<MedicalOrderGroupVo> getGroupedOrdersByPatient(String patientId) {
        // 1. 查询所有申请明细
        List<MedicalOrderItem> orderItems = medicalOrderItemMapper.selectByPatientId(patientId);
        if (CollectionUtils.isEmpty(orderItems)) {
            return new ArrayList<>();
        }

        // 2. 获取所有订单ID并查询订单
        Set<String> orderIds = orderItems.stream()
                .map(MedicalOrderItem::getOrderId)
                .collect(Collectors.toSet());

        List<MedicalOrder> orders = medicalOrderMapper.selectBatchIds(new ArrayList<>(orderIds));
        Map<String, MedicalOrder> orderMap = orders.stream()
                .collect(Collectors.toMap(MedicalOrder::getOrderId, o -> o, (v1, v2) -> v1));

        // 3. 获取所有挂号ID并查询挂号
        Set<String> registerIds = orders.stream()
                .map(MedicalOrder::getRegisterId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Map<String, Registration> registerMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(registerIds)) {
            List<Registration> registrations = registrationMapper.selectBatchIds(new ArrayList<>(registerIds));
            registerMap = registrations.stream()
                    .collect(Collectors.toMap(Registration::getRegisterId, r -> r, (v1, v2) -> v1));
        }

        // 4. 获取所有医生ID并查询医生
        Set<String> doctorIds = new HashSet<>();
        for (Registration reg : registerMap.values()) {
            if (StringUtils.hasText(reg.getDoctorId())) {
                doctorIds.add(reg.getDoctorId());
            }
        }
        for (MedicalOrder order : orders) {
            if (StringUtils.hasText(order.getDoctorId())) {
                doctorIds.add(order.getDoctorId());
            }
        }
        Map<String, Doctor> doctorMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(doctorIds)) {
            List<Doctor> doctors = doctorMapper.selectBatchIds(new ArrayList<>(doctorIds));
            doctorMap = doctors.stream()
                    .collect(Collectors.toMap(Doctor::getDoctorId, d -> d, (v1, v2) -> v1));
        }

        // 5. 获取所有报告
        Set<String> orderItemIds = orderItems.stream()
                .map(MedicalOrderItem::getOrderItemId)
                .collect(Collectors.toSet());
        Map<String, MedicalReport> reportMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(orderItemIds)) {
            List<MedicalReport> reports = medicalReportMapper.selectByOrderItemIds(new ArrayList<>(orderItemIds));
            reportMap = reports.stream()
                    .collect(Collectors.toMap(MedicalReport::getOrderItemId, r -> r, (v1, v2) -> v1));
        }

        // 6. 获取患者信息
        Patient patient = patientMapper.selectById(patientId);

        // 7. 按挂号分组
        Map<String, List<MedicalOrderItem>> groupMap = new LinkedHashMap<>();
        for (MedicalOrderItem item : orderItems) {
            MedicalOrder order = orderMap.get(item.getOrderId());
            if (order == null) continue;
            String registerId = StringUtils.hasText(order.getRegisterId())
                    ? order.getRegisterId()
                    : "no_register_" + order.getOrderId();
            groupMap.computeIfAbsent(registerId, k -> new ArrayList<>()).add(item);
        }

        // 8. 构建VO
        List<MedicalOrderGroupVo> result = new ArrayList<>();
        for (Map.Entry<String, List<MedicalOrderItem>> entry : groupMap.entrySet()) {
            String registerId = entry.getKey();
            List<MedicalOrderItem> items = entry.getValue();

            MedicalOrderGroupVo group = new MedicalOrderGroupVo();
            group.setRegisterId(registerId);

            Registration registration = registerMap.get(registerId);
            if (registration != null) {
                group.setPatientName(registration.getName());
                if (StringUtils.hasText(registration.getDoctorId())) {
                    Doctor doctor = doctorMap.get(registration.getDoctorId());
                    if (doctor != null) {
                        group.setDoctorName(doctor.getName());
                    }
                }
                if (registration.getVisitDate() != null) {
                    group.setVisitDate(registration.getVisitDate().format(DATE_FORMATTER));
                }
            }

            if (!StringUtils.hasText(group.getPatientName()) && patient != null) {
                group.setPatientName(patient.getName());
            }

            if (!StringUtils.hasText(group.getDoctorName())) {
                for (MedicalOrderItem item : items) {
                    MedicalOrder order = orderMap.get(item.getOrderId());
                    if (order != null && StringUtils.hasText(order.getDoctorId())) {
                        Doctor doctor = doctorMap.get(order.getDoctorId());
                        if (doctor != null) {
                            group.setDoctorName(doctor.getName());
                            break;
                        }
                    }
                }
            }

            if (!StringUtils.hasText(group.getVisitDate())) {
                for (MedicalOrderItem item : items) {
                    if (item.getCreateTime() != null) {
                        group.setVisitDate(item.getCreateTime().format(DATE_FORMATTER));
                        break;
                    }
                }
            }

            BigDecimal totalAmount = BigDecimal.ZERO;
            List<MedicalOrderItemVo> itemVos = new ArrayList<>();
            for (MedicalOrderItem item : items) {
                MedicalOrderItemVo vo = new MedicalOrderItemVo();
                BeanUtils.copyProperties(item, vo);
                vo.setOrderId(item.getOrderId());
                vo.setUrgencyLevel(URGENCY_TEXT.getOrDefault(item.getUrgencyLevel(), item.getUrgencyLevel()));
                vo.setStatus(STATUS_TEXT.getOrDefault(item.getStatus(), item.getStatus()));

                MedicalReport report = reportMap.get(item.getOrderItemId());
                if (report != null) {
                    vo.setReportId(report.getReportId());
                    vo.setReportStatus(STATUS_TEXT.getOrDefault(report.getStatus(), report.getStatus()));
                    vo.setConclusion(report.getConclusion());
                    vo.setAbnormalFlag(ABNORMAL_TEXT.getOrDefault(report.getAbnormalFlag(), report.getAbnormalFlag()));
                    vo.setReportTime(report.getReportTime());
                    vo.setFollowUpAdvice(report.getFollowUpAdvice());
                }

                if (item.getPrice() != null) {
                    totalAmount = totalAmount.add(item.getPrice());
                }
                itemVos.add(vo);
            }

            itemVos.sort((a, b) -> {
                if (a.getCreateTime() == null) return 1;
                if (b.getCreateTime() == null) return -1;
                return b.getCreateTime().compareTo(a.getCreateTime());
            });

            group.setItems(itemVos);
            group.setItemCount(itemVos.size());
            group.setTotalAmount(totalAmount);
            group.setStatus(calculateGroupStatus(itemVos));

            result.add(group);
        }

        result.sort((a, b) -> {
            if (a.getVisitDate() == null) return 1;
            if (b.getVisitDate() == null) return -1;
            return b.getVisitDate().compareTo(a.getVisitDate());
        });

        return result;
    }

    @Override
    public ReportDetailVo getReportDetail(String orderItemId) {
        // 1. 查询明细
        MedicalOrderItem orderItem = medicalOrderItemMapper.selectByOrderItemId(orderItemId);
        if (orderItem == null) {
            throw new RuntimeException("未找到该检查记录");
        }

        // 2. 查询订单
        MedicalOrder order = medicalOrderMapper.selectByOrderId(orderItem.getOrderId());
        if (order == null) {
            throw new RuntimeException("未找到申请记录");
        }

        // 3. 查询报告
        MedicalReport report = medicalReportMapper.selectByOrderItemId(orderItemId);

        // 4. 查询患者信息
        Patient patient = patientMapper.selectById(order.getPatientId());

        // 5. 查询挂号信息
        Registration registration = null;
        if (StringUtils.hasText(order.getRegisterId())) {
            registration = registrationMapper.selectById(order.getRegisterId());
        }

        // 6. 查询医生信息
        Doctor doctor = null;
        if (StringUtils.hasText(order.getDoctorId())) {
            doctor = doctorMapper.selectById(order.getDoctorId());
        }

        // 7. 构建 ReportDetailVo
        ReportDetailVo detail = new ReportDetailVo();
        BeanUtils.copyProperties(orderItem, detail);
        detail.setOrderItemId(orderItemId);
        detail.setOrderId(order.getOrderId());
        detail.setPatientId(order.getPatientId());
        detail.setUrgencyLevel(URGENCY_TEXT.getOrDefault(orderItem.getUrgencyLevel(), orderItem.getUrgencyLevel()));

        // 8. 填充患者信息
        if (patient != null) {
            detail.setPatientName(patient.getName());
        }

        // 9. 填充挂号信息
        if (registration != null) {
            detail.setRegisterId(registration.getRegisterId());
            if (!StringUtils.hasText(detail.getPatientName())) {
                detail.setPatientName(registration.getName());
            }
            if (StringUtils.hasText(registration.getDoctorId())) {
                Doctor regDoctor = doctorMapper.selectById(registration.getDoctorId());
                if (regDoctor != null) {
                    detail.setDoctorName(regDoctor.getName());
                }
            }
        }

        // 10. 如果医生姓名为空，使用订单中的医生
        if (!StringUtils.hasText(detail.getDoctorName()) && doctor != null) {
            detail.setDoctorName(doctor.getName());
        }

        // 11. 填充报告信息
        if (report != null) {
            BeanUtils.copyProperties(report, detail);
            detail.setAbnormalFlag(ABNORMAL_TEXT.getOrDefault(report.getAbnormalFlag(), report.getAbnormalFlag()));
            detail.setStatus(STATUS_TEXT.getOrDefault(report.getStatus(), report.getStatus()));
            detail.setFollowUpAdvice(report.getFollowUpAdvice());

            // 如果是检验项目，查询指标
            if ("LAB".equals(orderItem.getItemCategory())) {
                List<MedicalReportIndicator> indicators =
                        medicalReportIndicatorMapper.selectByReportId(report.getReportId());
                if (!CollectionUtils.isEmpty(indicators)) {
                    List<IndicatorVo> indicatorVos = indicators.stream().map(ind -> {
                        IndicatorVo vo = new IndicatorVo();
                        BeanUtils.copyProperties(ind, vo);
                        vo.setAbnormalFlag(ABNORMAL_TEXT.getOrDefault(ind.getAbnormalFlag(), ind.getAbnormalFlag()));
                        return vo;
                    }).collect(Collectors.toList());
                    detail.setIndicators(indicatorVos);
                }
            }
        }

        return detail;
    }

    private String calculateGroupStatus(List<MedicalOrderItemVo> items) {
        if (CollectionUtils.isEmpty(items)) return "待检查";

        boolean hasCancelled = false;
        boolean hasCompleted = true;
        boolean hasInProgress = false;
        boolean hasWaiting = false;

        for (MedicalOrderItemVo item : items) {
            String status = item.getStatus();
            if ("已取消".equals(status)) {
                hasCancelled = true;
            } else if ("已完成".equals(status)) {
                // do nothing
            } else if ("执行中".equals(status)) {
                hasInProgress = true;
                hasCompleted = false;
            } else {
                hasWaiting = true;
                hasCompleted = false;
            }
        }

        if (hasCompleted && !hasInProgress && !hasWaiting && !hasCancelled) return "已完成";
        if (hasInProgress) return "检查中";
        if (hasWaiting) return "待检查";
        if (hasCancelled && !hasCompleted && !hasInProgress && !hasWaiting) return "已取消";
        return "待检查";
    }
}
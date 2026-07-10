package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.doctor.dto.MedicalReportSubmitRequest;
import com.cloudbrainmed.doctor.entity.MedicalReport;
import com.cloudbrainmed.doctor.mapper.MedicalOrderMapper;
import com.cloudbrainmed.doctor.service.DoctorTaskService;
import com.cloudbrainmed.doctor.service.AgingService;
import com.cloudbrainmed.doctor.service.OrderItemService;
import com.cloudbrainmed.doctor.vo.DoctorTaskDetailVo;
import com.cloudbrainmed.doctor.vo.DoctorTaskVo;
import com.cloudbrainmed.doctor.vo.MedicalReportVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.hasText;

/**
 * 医生任务服务实现
 * 医生工作台、任务领取、完成任务、提交报告
 */
@Slf4j
@Service
public class DoctorTaskServiceImpl implements DoctorTaskService {

    private final MedicalOrderMapper medicalOrderMapper;
    private final OrderItemService orderItemService;
    private final AgingService agingService;

    public DoctorTaskServiceImpl(
            MedicalOrderMapper medicalOrderMapper,
            OrderItemService orderItemService,
            AgingService agingService) {
        this.medicalOrderMapper = medicalOrderMapper;
        this.orderItemService = orderItemService;
        this.agingService = agingService;
    }

    @Override
    public List<DoctorTaskVo> getDoctorWorkbench(String doctorId, Integer doctorType) {
        String itemCategory = null;
        if (Integer.valueOf(2).equals(doctorType)) {
            itemCategory = "EXAM";
        } else if (Integer.valueOf(3).equals(doctorType)) {
            itemCategory = "LAB";
        }
        return medicalOrderMapper.selectDoctorTasks(doctorId, itemCategory)
                .stream()
                .map(this::convertToVo)
                .collect(Collectors.toList());
    }

    @Override
    public DoctorTaskDetailVo getTaskDetail(String orderItemId, String doctorId) {
        MedicalOrderMapper.DoctorTaskDetailVo detail =
                medicalOrderMapper.selectTaskDetailById(orderItemId);
        if (detail == null) {
            throw new BusinessException("任务不存在");
        }
        return convertToDetailVo(detail);
    }

    @Override
    @Transactional
    public void startTask(String orderItemId, String doctorId) {
        MedicalOrderMapper.DoctorTaskDetailVo task =
                medicalOrderMapper.selectTaskDetailById(orderItemId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        String status = task.getStatus();
        if ("COMPLETED".equals(status)) {
            throw new BusinessException("该任务已完成");
        }
        if ("IN_PROCESS".equals(status)) {
            if (doctorId.equals(task.getAssignedDoctorId())) {
                log.info("Doctor {} already claimed task {}, idempotent return", doctorId, orderItemId);
                return;
            }
            throw new BusinessException("该任务已被其他医生领取");
        }
        if (!"QUEUED".equals(status)) {
            throw new BusinessException("任务状态异常（" + status + "），无法开始处理");
        }

        if (orderItemService.hasPatientInProgress(task.getPatientId())) {
            throw new BusinessException("该患者已有检查项目正在处理中，请等待完成后再处理");
        }
        if (orderItemService.hasDoctorInProgress(doctorId)) {
            throw new BusinessException("您已有正在处理的任务，请先完成当前任务");
        }

        boolean claimed = orderItemService.claimTask(orderItemId, doctorId);
        if (!claimed) {
            throw new BusinessException("任务已被其他医生领取");
        }
        log.info("Doctor {} started task {} for patient {}", doctorId, orderItemId, task.getPatientId());
    }

    @Override
    @Transactional
    public void completeTask(String orderItemId, String doctorId) {
        if (!orderItemService.hasPublishedReport(orderItemId)) {
            throw new BusinessException("请先提交并发布检查检验报告");
        }
        int updated = medicalOrderMapper.completeTask(orderItemId, doctorId);
        if (updated == 0) {
            throw new BusinessException("任务状态异常或不属于当前医生，无法完成");
        }
        log.info("Doctor {} completed task {}", doctorId, orderItemId);
    }

    @Override
    @Transactional
    public void skipTask(String orderItemId, String doctorId) {
        MedicalOrderMapper.DoctorTaskDetailVo task =
                medicalOrderMapper.selectTaskDetailById(orderItemId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        if (!"IN_PROCESS".equals(task.getStatus())) {
            throw new BusinessException("只能跳过处理中的任务");
        }
        if (!doctorId.equals(task.getAssignedDoctorId())) {
            throw new BusinessException("该任务不属于当前医生");
        }
        if (orderItemService.hasPublishedReport(orderItemId)) {
            throw new BusinessException("该任务已提交报告，不可跳过");
        }

        boolean released = orderItemService.releaseTask(orderItemId, doctorId);
        if (!released) {
            throw new BusinessException("跳过任务失败，请重试");
        }
        log.info("Doctor {} skipped task {} ({}), back to queue",
                doctorId, orderItemId, task.getItemName());
    }

    @Override
    @Transactional
    public MedicalReportVo submitReport(MedicalReportSubmitRequest request, String doctorId) {
        MedicalOrderMapper.DoctorTaskDetailVo task =
                medicalOrderMapper.selectTaskDetailById(request.getOrderItemId());
        if (task == null) {
            throw new BusinessException("task not found");
        }
        if (!"IN_PROCESS".equals(task.getStatus())) {
            throw new BusinessException("task is not in process");
        }
        if (!doctorId.equals(task.getAssignedDoctorId())) {
            throw new BusinessException("task does not belong to current doctor");
        }
        if (!hasText(request.getResultSummary()) && !hasText(request.getConclusion())) {
            throw new BusinessException("report summary or conclusion is required");
        }

        LocalDateTime now = LocalDateTime.now();
        MedicalReport report = new MedicalReport();
        report.setReportId(newId("MR", 30));
        report.setOrderItemId(task.getOrderItemId());
        report.setPatientId(task.getPatientId());
        report.setItemCategory(task.getItemCategory());
        report.setResultSummary(blankToNull(request.getResultSummary()));
        report.setConclusion(blankToNull(request.getConclusion()));
        report.setAbnormalFlag(normalizeAbnormalFlag(request.getAbnormalFlag()));
        report.setAttachmentUrl(blankToNull(request.getAttachmentUrl()));
        report.setReportDoctorId(doctorId);
        report.setStatus("PUBLISHED");
        report.setPerformedTime(now);
        report.setReportTime(now);
        report.setCreateTime(now);
        report.setUpdateTime(now);
        if (medicalOrderMapper.insertMedicalReport(report) != 1) {
            throw new BusinessException("save report failed");
        }
        medicalOrderMapper.completeTask(task.getOrderItemId(), doctorId);
        return toReportVo(report, task);
    }

    private DoctorTaskVo convertToVo(MedicalOrderMapper.DoctorTaskVo task) {
        DoctorTaskVo vo = new DoctorTaskVo();
        vo.setOrderItemId(task.getOrderItemId());
        vo.setOrderId(task.getOrderId());
        vo.setItemCode(task.getItemCode());
        vo.setItemName(task.getItemName());
        vo.setItemCategory(task.getItemCategory());
        vo.setUrgencyLevel(task.getUrgencyLevel());
        vo.setPrice(task.getPrice());
        vo.setStatus(task.getStatus());
        vo.setStatusLabel(mapStatusLabel(task.getStatus()));
        vo.setCreateTime(task.getCreateTime() != null ? task.getCreateTime().toString() : null);
        vo.setAssignTime(task.getAssignTime() != null ? task.getAssignTime().toString() : null);
        vo.setPatientId(task.getPatientId());
        vo.setRegisterId(task.getRegisterId());
        vo.setPatientName(task.getPatientName());
        vo.setGender(task.getGender());
        vo.setAge(task.getAge());

        // 计算等待分钟数
        if (task.getCreateTime() != null) {
            vo.setWaitingMinutes(Duration.between(task.getCreateTime(), LocalDateTime.now()).toMinutes());
        }
        // 老化优先级：NORMAL 超过30分钟 → 显示为加急
        boolean agingActive = agingService.isAgingThresholdReached(task.getCreateTime());
        if ("NORMAL".equals(task.getUrgencyLevel()) && agingActive) {
            vo.setUrgencyLabel("加急↑");
            vo.setAgingPromoted(true);
        } else {
            vo.setUrgencyLabel(mapUrgencyLabel(task.getUrgencyLevel()));
            vo.setAgingPromoted(false);
        }
        return vo;
    }

    private DoctorTaskDetailVo convertToDetailVo(MedicalOrderMapper.DoctorTaskDetailVo detail) {
        DoctorTaskDetailVo vo = new DoctorTaskDetailVo();

        vo.setAssignedDoctorName(detail.getAssignedDoctorName());
        vo.setOrderItemId(detail.getOrderItemId());
        vo.setOrderId(detail.getOrderId());
        vo.setItemCode(detail.getItemCode());
        vo.setItemName(detail.getItemName());
        vo.setItemCategory(detail.getItemCategory());
        vo.setUrgencyLevel(detail.getUrgencyLevel());
        vo.setUrgencyLabel(mapUrgencyLabel(detail.getUrgencyLevel()));
        vo.setPrice(detail.getPrice());
        vo.setStatus(detail.getStatus());
        vo.setStatusLabel(mapStatusLabel(detail.getStatus()));
        vo.setCreateTime(detail.getCreateTime());
        vo.setAssignTime(detail.getAssignTime());
        vo.setCompleteTime(detail.getCompleteTime());
        vo.setPatientId(detail.getPatientId());
        vo.setRegisterId(detail.getRegisterId());
        vo.setRequesterDoctorId(detail.getRequesterDoctorId());
        vo.setPatientName(detail.getPatientName());
        vo.setGender(detail.getGender());
        vo.setAge(detail.getAge());
        vo.setClinicalSummary(detail.getClinicalSummary());
        return vo;
    }

    private MedicalReportVo toReportVo(MedicalReport report, MedicalOrderMapper.DoctorTaskDetailVo task) {
        MedicalReportVo vo = new MedicalReportVo();
        vo.setReportId(report.getReportId());
        vo.setOrderItemId(report.getOrderItemId());
        vo.setOrderId(task.getOrderId());
        vo.setRegisterId(task.getRegisterId());
        vo.setPatientId(report.getPatientId());
        vo.setItemCode(task.getItemCode());
        vo.setItemName(task.getItemName());
        vo.setItemCategory(report.getItemCategory());
        vo.setResultSummary(report.getResultSummary());
        vo.setConclusion(report.getConclusion());
        vo.setAbnormalFlag(report.getAbnormalFlag());
        vo.setAttachmentUrl(report.getAttachmentUrl());
        vo.setReportDoctorId(report.getReportDoctorId());
        vo.setStatus(report.getStatus());
        vo.setPerformedTime(report.getPerformedTime());
        vo.setReportTime(report.getReportTime());
        return vo;
    }

    private String mapUrgencyLabel(String level) {
        switch (level) {
            case "EMERGENCY": return "紧急";
            case "URGENT": return "加急";
            case "NORMAL": return "常规";
            default: return level;
        }
    }

    private String mapStatusLabel(String status) {
        switch (status) {
            case "WAITING_ASSIGN": return "待分配";
            case "QUEUED": return "排队中";
            case "IN_PROCESS": return "处理中";
            case "COMPLETED": return "已完成";
            case "CANCELLED": return "已取消";
            default: return status;
        }
    }

    private String newId(String prefix, int length) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, length - prefix.length() - 1);
    }

    private String normalizeAbnormalFlag(String flag) {
        if (flag == null) return null;
        switch (flag) {
            case "normal": return "NORMAL";
            case "abnormal": return "ABNORMAL";
            default: return flag;
        }
    }

    /** 如果字符串为 null 或空白则返回 null，否则返回 trim 后的值 */
    private static String blankToNull(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        return s.trim();
    }
}

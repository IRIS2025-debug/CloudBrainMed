package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.doctor.dto.MedicalReportSubmitRequest;
import com.cloudbrainmed.doctor.entity.MedicalReport;
import com.cloudbrainmed.doctor.mapper.MedicalOrderMapper;
import com.cloudbrainmed.doctor.service.DoctorTaskService;
import com.cloudbrainmed.doctor.service.OrderItemService;
import com.cloudbrainmed.doctor.vo.DoctorTaskDetailVo;
import com.cloudbrainmed.doctor.vo.MedicalReportVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.util.StringUtils.hasText;

@Slf4j
@Service
public class DoctorTaskServiceImpl implements DoctorTaskService {

    private final MedicalOrderMapper medicalOrderMapper;
    private final OrderItemService orderItemService;

    public DoctorTaskServiceImpl(
            MedicalOrderMapper medicalOrderMapper,
            OrderItemService orderItemService) {
        this.medicalOrderMapper = medicalOrderMapper;
        this.orderItemService = orderItemService;
    }

    @Override
    public DoctorTaskDetailVo getTaskDetail(
            String orderItemId, String doctorId, Integer doctorType) {
        MedicalOrderMapper.DoctorTaskDetailVo detail =
                medicalOrderMapper.selectTaskDetailById(orderItemId);
        if (detail == null) {
            throw new BusinessException("任务不存在");
        }
        requireDoctorTypeForItem(detail.getItemCategory(), doctorType);
        if (detail.getAssignedDoctorId() != null
                && !doctorId.equals(detail.getAssignedDoctorId())
                && !"COMPLETED".equals(detail.getStatus())) {
            throw new BusinessException("该任务不属于当前医生");
        }
        DoctorTaskDetailVo vo = convertToDetailVo(detail);
        if ("COMPLETED".equals(detail.getStatus())) {
            vo.setReport(medicalOrderMapper.findPublishedReportByOrderItemId(orderItemId));
        }
        return vo;
    }

    @Override
    @Transactional
    public void startTask(String orderItemId, String doctorId, Integer doctorType) {
        MedicalOrderMapper.DoctorTaskDetailVo task =
                medicalOrderMapper.selectTaskDetailById(orderItemId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        requireDoctorTypeForItem(task.getItemCategory(), doctorType);

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

        boolean claimed = orderItemService.claimTask(orderItemId, doctorId);
        if (!claimed) {
            throw new BusinessException("任务已被其他医生领取");
        }
        log.info("Doctor {} started task {} for patient {}", doctorId, orderItemId, task.getPatientId());
    }

    @Override
    @Transactional
    public void completeTask(String orderItemId, String doctorId, Integer doctorType) {
        MedicalOrderMapper.DoctorTaskDetailVo task =
                medicalOrderMapper.selectTaskDetailById(orderItemId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        requireDoctorTypeForItem(task.getItemCategory(), doctorType);
        if (!orderItemService.hasPublishedReport(orderItemId)) {
            throw new BusinessException("请先提交并发布检查/检验报告");
        }
        int updated = medicalOrderMapper.completeTask(orderItemId, doctorId);
        if (updated == 0) {
            throw new BusinessException("任务状态异常或不属于当前医生，无法完成");
        }
        log.info("Doctor {} completed task {}", doctorId, orderItemId);
    }

    @Override
    @Transactional
    public MedicalReportVo submitReport(
            MedicalReportSubmitRequest request, String doctorId, Integer doctorType) {
        MedicalOrderMapper.DoctorTaskDetailVo task =
                medicalOrderMapper.selectTaskDetailById(request.getOrderItemId());
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        requireDoctorTypeForItem(task.getItemCategory(), doctorType);
        if (!"IN_PROCESS".equals(task.getStatus())) {
            throw new BusinessException("任务不在处理中");
        }
        if (!doctorId.equals(task.getAssignedDoctorId())) {
            throw new BusinessException("该任务不属于当前医生");
        }
        if (!hasText(request.getResultSummary()) && !hasText(request.getConclusion())) {
            throw new BusinessException("报告结果或诊断意见不能为空");
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
        report.setAiResultJson(blankToNull(request.getAiResultJson()));
        report.setReportDoctorId(doctorId);
        report.setStatus("PUBLISHED");
        report.setPerformedTime(now);
        report.setReportTime(now);
        report.setCreateTime(now);
        report.setUpdateTime(now);
        if (medicalOrderMapper.insertMedicalReport(report) != 1) {
            throw new BusinessException("保存报告失败");
        }
        medicalOrderMapper.completeTask(task.getOrderItemId(), doctorId);
        return toReportVo(report, task);
    }

    private DoctorTaskDetailVo convertToDetailVo(MedicalOrderMapper.DoctorTaskDetailVo detail) {
        DoctorTaskDetailVo vo = new DoctorTaskDetailVo();
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
        vo.setAssignedDoctorId(detail.getAssignedDoctorId());
        vo.setPatientId(detail.getPatientId());
        vo.setRegisterId(detail.getRegisterId());
        vo.setRequesterDoctorId(detail.getRequesterDoctorId());
        vo.setRequesterDoctorName(detail.getRequesterDoctorName());
        vo.setAssignedDeptName(detail.getAssignedDeptName());
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
        vo.setAiResultJson(report.getAiResultJson());
        vo.setReportDoctorId(report.getReportDoctorId());
        vo.setStatus(report.getStatus());
        vo.setPerformedTime(report.getPerformedTime());
        vo.setReportTime(report.getReportTime());
        return vo;
    }

    private String mapUrgencyLabel(String level) {
        if ("EMERGENCY".equals(level)) {
            return "紧急";
        }
        if ("URGENT".equals(level)) {
            return "加急";
        }
        if ("NORMAL".equals(level)) {
            return "常规";
        }
        return level;
    }

    private void requireDoctorTypeForItem(String itemCategory, Integer doctorType) {
        boolean allowed = ("EXAM".equals(itemCategory) && Integer.valueOf(2).equals(doctorType))
                || ("LAB".equals(itemCategory) && Integer.valueOf(3).equals(doctorType));
        if (!allowed) {
            throw new BusinessException("无权处理该检查/检验任务");
        }
    }

    private String mapStatusLabel(String status) {
        if ("WAITING_ASSIGN".equals(status)) {
            return "待分配";
        }
        if ("QUEUED".equals(status)) {
            return "排队中";
        }
        if ("IN_PROCESS".equals(status)) {
            return "处理中";
        }
        if ("COMPLETED".equals(status)) {
            return "已完成";
        }
        if ("CANCELLED".equals(status)) {
            return "已取消";
        }
        return status;
    }

    private String newId(String prefix, int length) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, length - prefix.length() - 1);
    }

    private String normalizeAbnormalFlag(String flag) {
        if (flag == null) {
            return null;
        }
        if ("normal".equals(flag)) {
            return "NORMAL";
        }
        if ("abnormal".equals(flag)) {
            return "ABNORMAL";
        }
        return flag;
    }

    private static String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}

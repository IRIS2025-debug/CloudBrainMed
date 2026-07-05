package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.doctor.dto.MedicalReportSubmitRequest;
import com.cloudbrainmed.doctor.entity.MedicalReport;
import com.cloudbrainmed.doctor.mapper.DoctorSkillMapper;
import com.cloudbrainmed.doctor.mapper.MedicalOrderMapper;
import com.cloudbrainmed.doctor.mapper.MedicalOrderMapper.QueuedTaskItem;
import com.cloudbrainmed.doctor.mapper.DoctorSkillMapper.DoctorSkillMatch;
import com.cloudbrainmed.doctor.service.TaskSchedulerService;
import com.cloudbrainmed.doctor.vo.DoctorTaskDetailVo;
import com.cloudbrainmed.doctor.vo.DoctorTaskVo;
import com.cloudbrainmed.doctor.vo.MedicalReportVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 任务调度服务实现
 * ========================
 * 核心调度规则：
 * 1. queued 按 urgency_level + create_time 排序
 * 2. 同 patient_id 只能一个 in_process
 * 3. doctor 同时只能处理一个任务
 * 4. 防止重复分配（乐观锁 WHERE status = 'QUEUED'）
 * 5. 通过 doctor_skill 表匹配医生技能
 */
@Slf4j
@Service
public class TaskSchedulerServiceImpl implements TaskSchedulerService {

    private final MedicalOrderMapper medicalOrderMapper;
    private final DoctorSkillMapper doctorSkillMapper;

    private static final int BATCH_SIZE = 100;

    public TaskSchedulerServiceImpl(
            MedicalOrderMapper medicalOrderMapper,
            DoctorSkillMapper doctorSkillMapper) {
        this.medicalOrderMapper = medicalOrderMapper;
        this.doctorSkillMapper = doctorSkillMapper;
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
    public List<DoctorTaskVo> getAssignableQueue(Integer doctorType) {
        String itemCategory = itemCategoryForDoctorType(doctorType);
        return medicalOrderMapper.findQueuedTasksByCategory(itemCategory, BATCH_SIZE)
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
        if (!"QUEUED".equals(task.getStatus())) {
            throw new BusinessException("任务状态异常，无法开始处理");
        }
        int inProcessByPatient = medicalOrderMapper.countInProgressByPatient(task.getPatientId());
        if (inProcessByPatient > 0) {
            throw new BusinessException("该患者已有检查项目正在处理中，请等待完成后再处理");
        }
        int inProcessByDoctor = medicalOrderMapper.countInProgressByDoctor(doctorId);
        if (inProcessByDoctor > 0) {
            throw new BusinessException("您已有正在处理的任务，请先完成当前任务");
        }

        int updated = medicalOrderMapper.updateItemStatus(
                orderItemId, "QUEUED", "IN_PROCESS");
        if (updated == 0) {
            throw new BusinessException("任务已被其他医生领取");
        }
        medicalOrderMapper.assignDoctor(orderItemId, doctorId);
        log.info("Doctor {} started task {} for patient {}", doctorId, orderItemId, task.getPatientId());
    }

    @Override
    @Transactional
    public void completeTask(String orderItemId, String doctorId) {
        if (medicalOrderMapper.countPublishedReportsByOrderItemId(orderItemId) <= 0) {
            throw new BusinessException("请先提交并发布检查检验报告");
        }
        markTaskCompleted(orderItemId, doctorId);
    }

    private void markTaskCompleted(String orderItemId, String doctorId) {
        int updated = medicalOrderMapper.completeTask(orderItemId, doctorId);
        if (updated == 0) {
            throw new BusinessException("任务状态异常或不属于当前医生，无法完成");
        }
        log.info("Doctor {} completed task {}", doctorId, orderItemId);
    }

    @Override
    @Transactional
    public MedicalReportVo submitReport(
            MedicalReportSubmitRequest request, String doctorId) {
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
        if (!hasText(request.getResultSummary())
                && !hasText(request.getConclusion())) {
            throw new BusinessException("report summary or conclusion is required");
        }

        LocalDateTime now = LocalDateTime.now();
        MedicalReport report = new MedicalReport();
        report.setReportId(newId("MR", 30));
        report.setOrderItemId(task.getOrderItemId());
        report.setPatientId(task.getPatientId());
        report.setItemCategory(task.getItemCategory());
        report.setResultSummary(trimToNull(request.getResultSummary()));
        report.setConclusion(trimToNull(request.getConclusion()));
        report.setAbnormalFlag(normalizeAbnormalFlag(request.getAbnormalFlag()));
        report.setAttachmentUrl(trimToNull(request.getAttachmentUrl()));
        report.setReportDoctorId(doctorId);
        report.setStatus("PUBLISHED");
        report.setPerformedTime(now);
        report.setReportTime(now);
        report.setCreateTime(now);
        report.setUpdateTime(now);
        if (medicalOrderMapper.insertMedicalReport(report) != 1) {
            throw new BusinessException("save report failed");
        }
        markTaskCompleted(task.getOrderItemId(), doctorId);
        return toReportVo(report, task);
    }

    @Override
    @Transactional
    public void enqueueByPayment(String orderId) {
        int paid = medicalOrderMapper.updatePayStatus(orderId);
        if (paid == 0) {
            log.info("Payment callback: order {} was already marked paid or not found", orderId);
        }
        int queuedOrder = medicalOrderMapper.enqueueOrder(orderId);
        int enqueued = medicalOrderMapper.enqueueOrderItems(orderId);
        log.info("Order {} paid, main order queued={}, {} items enqueued",
                orderId, queuedOrder, enqueued);
    }

    @Override
    @Transactional
    public int runScheduler() {
        List<QueuedTaskItem> queuedTasks = medicalOrderMapper.findQueuedTasks(BATCH_SIZE);
        if (queuedTasks.isEmpty()) {
            return 0;
        }

        int assigned = 0;
        Set<String> busyPatients = new HashSet<>();
        Set<String> busyDoctors = new HashSet<>();

        for (QueuedTaskItem task : queuedTasks) {
            if (busyPatients.contains(task.getPatientId())) {
                continue;
            }

            int patientInProcess = medicalOrderMapper.countInProgressByPatient(task.getPatientId());
            if (patientInProcess > 0) {
                busyPatients.add(task.getPatientId());
                continue;
            }

            String assignedDoctor = findAvailableDoctor(task, busyDoctors);
            if (assignedDoctor == null) {
                continue;
            }

            int updated = medicalOrderMapper.updateItemStatus(
                    task.getOrderItemId(), "QUEUED", "IN_PROCESS");
            if (updated == 0) {
                continue;
            }
            medicalOrderMapper.assignDoctor(task.getOrderItemId(), assignedDoctor);
            busyPatients.add(task.getPatientId());
            busyDoctors.add(assignedDoctor);
            assigned++;
            log.info("Scheduler: task {} assigned to doctor {} for patient {}",
                    task.getOrderItemId(), assignedDoctor, task.getPatientId());
        }

        return assigned;
    }

    @Override
    public long getQueueCount() {
        return medicalOrderMapper.countQueuedTasks();
    }

    @Override
    public long getAssignableQueueCount(Integer doctorType) {
        return medicalOrderMapper.countQueuedTasksByCategory(
                itemCategoryForDoctorType(doctorType));
    }

    private String itemCategoryForDoctorType(Integer doctorType) {
        if (Integer.valueOf(2).equals(doctorType)) {
            return "EXAM";
        }
        if (Integer.valueOf(3).equals(doctorType)) {
            return "LAB";
        }
        throw new BusinessException("unsupported doctor type");
    }

    private String findAvailableDoctor(QueuedTaskItem task, Set<String> busyDoctors) {
        Integer doctorType = "EXAM".equals(task.getItemCategory()) ? 2 :
                             "LAB".equals(task.getItemCategory()) ? 3 : null;
        List<DoctorSkillMatch> candidates =
                doctorSkillMapper.findMatchingDoctorsByItemCode(task.getItemCode(), doctorType);
        if (candidates == null || candidates.isEmpty()) {
            log.warn("No doctor with skill for itemCode={} itemCategory={}", task.getItemCode(), task.getItemCategory());
            return null;
        }

        for (DoctorSkillMatch candidate : candidates) {
            if (busyDoctors.contains(candidate.getDoctorId())) {
                continue;
            }
            int inProcessCount = medicalOrderMapper.countInProgressByDoctor(
                    candidate.getDoctorId());
            if (inProcessCount > 0) {
                busyDoctors.add(candidate.getDoctorId());
                continue;
            }
            return candidate.getDoctorId();
        }

        return null;
    }

    private DoctorTaskVo convertToVo(MedicalOrderMapper.DoctorTaskVo source) {
        DoctorTaskVo vo = new DoctorTaskVo();
        vo.setOrderItemId(source.getOrderItemId());
        vo.setOrderId(source.getOrderId());
        vo.setItemCode(source.getItemCode());
        vo.setItemName(source.getItemName());
        vo.setItemCategory(source.getItemCategory());
        vo.setUrgencyLevel(source.getUrgencyLevel());
        vo.setPrice(source.getPrice());
        vo.setStatus(source.getStatus());
        vo.setCreateTime(source.getCreateTime());
        vo.setAssignTime(source.getAssignTime());
        vo.setPatientId(source.getPatientId());
        vo.setRegisterId(source.getRegisterId());
        vo.setPatientName(source.getPatientName());
        vo.setGender(source.getGender());
        vo.setAge(source.getAge());
        vo.setClinicalSummary(source.getClinicalSummary());
        return vo;
    }

    private DoctorTaskVo convertToVo(QueuedTaskItem source) {
        DoctorTaskVo vo = new DoctorTaskVo();
        vo.setOrderItemId(source.getOrderItemId());
        vo.setOrderId(source.getOrderId());
        vo.setItemCode(source.getItemCode());
        vo.setItemName(source.getItemName());
        vo.setItemCategory(source.getItemCategory());
        vo.setUrgencyLevel(source.getUrgencyLevel());
        vo.setPrice(source.getPrice());
        vo.setStatus(source.getStatus());
        vo.setCreateTime(source.getCreateTime());
        vo.setPatientId(source.getPatientId());
        vo.setRegisterId(source.getRegisterId());
        vo.setPatientName(source.getPatientName());
        vo.setGender(source.getGender());
        vo.setAge(source.getAge());
        return vo;
    }

    private DoctorTaskDetailVo convertToDetailVo(MedicalOrderMapper.DoctorTaskDetailVo source) {
        DoctorTaskDetailVo vo = new DoctorTaskDetailVo();
        vo.setOrderItemId(source.getOrderItemId());
        vo.setOrderId(source.getOrderId());
        vo.setItemCode(source.getItemCode());
        vo.setItemName(source.getItemName());
        vo.setItemCategory(source.getItemCategory());
        vo.setUrgencyLevel(source.getUrgencyLevel());
        vo.setPrice(source.getPrice());
        vo.setStatus(source.getStatus());
        vo.setCreateTime(source.getCreateTime());
        vo.setAssignTime(source.getAssignTime());
        vo.setCompleteTime(source.getCompleteTime());
        vo.setPatientId(source.getPatientId());
        vo.setRegisterId(source.getRegisterId());
        vo.setRequesterDoctorId(source.getRequesterDoctorId());
        vo.setPatientName(source.getPatientName());
        vo.setGender(source.getGender());
        vo.setBirthday(source.getBirthday());
        vo.setAge(source.getAge());
        vo.setClinicalSummary(source.getClinicalSummary());
        return vo;
    }

    private MedicalReportVo toReportVo(
            MedicalReport report,
            MedicalOrderMapper.DoctorTaskDetailVo task) {
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

    private String normalizeAbnormalFlag(String value) {
        String flag = trimToNull(value);
        return flag == null ? "NORMAL" : flag.toUpperCase();
    }

    private String trimToNull(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String newId(String prefix, int randomLength) {
        return prefix + UUID.randomUUID().toString()
                .replace("-", "").substring(0, randomLength);
    }
}

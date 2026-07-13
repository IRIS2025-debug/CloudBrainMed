package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.doctor.entity.DoctorSkill;
import com.cloudbrainmed.doctor.mapper.DoctorSkillMapper;
import com.cloudbrainmed.doctor.mapper.MedicalOrderMapper;
import com.cloudbrainmed.doctor.mapper.MedicalOrderMapper.QueuedTaskItem;
import com.cloudbrainmed.doctor.service.AgingService;
import com.cloudbrainmed.doctor.service.DoctorScheduleService;
import com.cloudbrainmed.doctor.service.QueueService;
import com.cloudbrainmed.doctor.vo.DoctorTaskVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 队列服务实现
 * 排队任务查询、数量统计、医生技能匹配
 */
@Slf4j
@Service
public class QueueServiceImpl implements QueueService {

    private final MedicalOrderMapper medicalOrderMapper;
    private final DoctorSkillMapper doctorSkillMapper;
    private final DoctorScheduleService doctorScheduleService;
    private final AgingService agingService;

    public QueueServiceImpl(
            MedicalOrderMapper medicalOrderMapper,
            DoctorSkillMapper doctorSkillMapper,
            DoctorScheduleService doctorScheduleService,
            AgingService agingService) {
        this.medicalOrderMapper = medicalOrderMapper;
        this.doctorSkillMapper = doctorSkillMapper;
        this.doctorScheduleService = doctorScheduleService;
        this.agingService = agingService;
    }

    @Override
    public List<DoctorTaskVo> getAssignableQueue(String doctorId, Integer doctorType) {
        // 1. 获取医生的技能项目编码
        Set<String> skilledItemCodes = getDoctorSkilledItemCodes(doctorId);

        // 2. 按类别获取排队任务（降级策略：无技能配置时按类别过滤全部返回）
        String itemCategory = itemCategoryForDoctorType(doctorType);
        List<QueuedTaskItem> tasks = medicalOrderMapper.findQueuedTasksByCategory(itemCategory, 100);

        // 3. 如果医生配置了技能，则只保留技能匹配的任务
        if (!skilledItemCodes.isEmpty()) {
            tasks = tasks.stream()
                    .filter(t -> skilledItemCodes.contains(t.getItemCode()))
                    .collect(Collectors.toList());
        }

        return tasks.stream()
                .map(this::convertToVo)
                .collect(Collectors.toList());
    }

    @Override
    public List<QueuedTaskItem> getQueuedTasks(int limit) {
        List<QueuedTaskItem> rawTasks = medicalOrderMapper.findQueuedTasks(limit);
        return agingService.sortWithAging(rawTasks);
    }

    @Override
    public long getQueueCount() {
        return medicalOrderMapper.countQueuedTasks();
    }

    @Override
    public long getAssignableQueueCount(String doctorId, Integer doctorType) {
        // 与 getAssignableQueue 一致的过滤逻辑，确保数量与表格行数匹配
        Set<String> skilledItemCodes = getDoctorSkilledItemCodes(doctorId);
        String itemCategory = itemCategoryForDoctorType(doctorType);
        List<QueuedTaskItem> tasks = medicalOrderMapper.findQueuedTasksByCategory(itemCategory, 100);
        if (!skilledItemCodes.isEmpty()) {
            tasks = tasks.stream()
                    .filter(t -> skilledItemCodes.contains(t.getItemCode()))
                    .collect(Collectors.toList());
        }
        return tasks.size();
    }

    @Override
    public String findAvailableDoctor(QueuedTaskItem task, Set<String> busyDoctors) {
        Integer doctorType = "EXAM".equals(task.getItemCategory()) ? 2 :
                             "LAB".equals(task.getItemCategory()) ? 3 : null;
        log.info("Scheduler candidate task orderItemId={}, itemCode={}, itemCategory={}, doctorType={}, busyDoctors={}",
                task.getOrderItemId(), task.getItemCode(), task.getItemCategory(), doctorType, busyDoctors);

        List<DoctorSkillMapper.DoctorSkillMatch> matches =
                doctorSkillMapper.findMatchingDoctorsByItemCode(
                        task.getItemCode(), doctorType);
        if (matches == null || matches.isEmpty()) {
            log.warn("No doctor with skill for itemCode={} itemCategory={}",
                    task.getItemCode(), task.getItemCategory());
            return null;
        }

        List<String> candidateDoctorIds = matches.stream()
                .map(DoctorSkillMapper.DoctorSkillMatch::getDoctorId)
                .distinct()
                .collect(Collectors.toList());
        log.info("Skill matched doctors for orderItemId={} => {}", task.getOrderItemId(), candidateDoctorIds);

        Set<String> availableDoctorIds = doctorScheduleService.filterAvailableDoctors(
                candidateDoctorIds, LocalDateTime.now());
        log.info("Schedule matched doctors for orderItemId={} => {}", task.getOrderItemId(), availableDoctorIds);
        if (availableDoctorIds.isEmpty()) {
            log.warn("No doctor in schedule for itemCode={} itemCategory={} candidates={}",
                    task.getItemCode(), task.getItemCategory(), candidateDoctorIds);
            return null;
        }

        for (DoctorSkillMapper.DoctorSkillMatch match : matches) {
            String doctorId = match.getDoctorId();
            if (!availableDoctorIds.contains(doctorId)) {
                continue;
            }
            if (busyDoctors.contains(doctorId)) {
                log.info("Skip doctor {} for orderItemId={} because busyDoctors contains it",
                        doctorId, task.getOrderItemId());
                continue;
            }
            // 检查医生是否已有处理中的任务
            int inProgressCount = medicalOrderMapper.countInProgressByDoctor(doctorId);
            log.info("Doctor {} in-progress count for orderItemId={} => {}",
                    doctorId, task.getOrderItemId(), inProgressCount);
            if (inProgressCount > 0) {
                busyDoctors.add(doctorId);
                continue;
            }
            log.info("Doctor {} selected for orderItemId={}", doctorId, task.getOrderItemId());
            return doctorId;
        }
        log.warn("No available doctor found for orderItemId={} itemCode={} itemCategory={}",
                task.getOrderItemId(), task.getItemCode(), task.getItemCategory());
        return null;
    }

    @Override
    public String itemCategoryForDoctorType(Integer doctorType) {
        if (Integer.valueOf(2).equals(doctorType)) {
            return "EXAM";
        } else if (Integer.valueOf(3).equals(doctorType)) {
            return "LAB";
        }
        return null;
    }

    private DoctorTaskVo convertToVo(QueuedTaskItem item) {
        DoctorTaskVo vo = new DoctorTaskVo();
        vo.setOrderItemId(item.getOrderItemId());
        vo.setOrderId(item.getOrderId());
        vo.setItemCode(item.getItemCode());
        vo.setItemName(item.getItemName());
        vo.setItemCategory(item.getItemCategory());
        vo.setUrgencyLevel(item.getUrgencyLevel());
        vo.setPrice(item.getPrice());
        vo.setStatus(item.getStatus());
        vo.setStatusLabel(mapStatusLabel(item.getStatus()));
        vo.setCreateTime(item.getCreateTime() != null ? item.getCreateTime().toString() : null);
        vo.setPatientId(item.getPatientId());
        vo.setRegisterId(item.getRegisterId());
        vo.setPatientName(item.getPatientName());
        vo.setGender(item.getGender());
        vo.setAge(item.getAge());

        // 计算等待分钟数
        if (item.getCreateTime() != null) {
            vo.setWaitingMinutes(Duration.between(item.getCreateTime(), LocalDateTime.now()).toMinutes());
        }
        // 老化优先级：NORMAL 超过30分钟 → 显示为加急↑
        String effectiveLevel = agingService.resolveEffectiveUrgencyLevel(
                item.getUrgencyLevel(), item.getCreateTime());
        boolean agingActive = !effectiveLevel.equals(item.getUrgencyLevel());
        if ("NORMAL".equals(item.getUrgencyLevel()) && agingActive) {
            vo.setUrgencyLabel("加急↑");
            vo.setAgingPromoted(true);
        } else {
            vo.setUrgencyLabel(mapUrgencyLabel(effectiveLevel));
            vo.setAgingPromoted(false);
        }
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

    /**
     * 查询医生擅长的项目编码列表
     * 如果未配置技能则返回空集合（降级为按类别显示）
     */
    private Set<String> getDoctorSkilledItemCodes(String doctorId) {
        List<DoctorSkill> skills = doctorSkillMapper.selectByDoctorId(doctorId);
        if (skills == null || skills.isEmpty()) {
            return Collections.emptySet();
        }
        return skills.stream()
                .map(DoctorSkill::getItemCode)
                .collect(Collectors.toSet());
    }
}
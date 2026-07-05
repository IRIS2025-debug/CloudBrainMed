package com.cloudbrainmed.doctor.service;

import com.cloudbrainmed.doctor.mapper.MedicalOrderMapper.QueuedTaskItem;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 防饿死服务（逻辑层，不改DB）
 * 基于 create_time 实现等待时间 > 30分钟 → 提升优先级
 */
public interface AgingService {

    /**
     * 防饿死排序：对排队任务应用时效老化策略
     * 规则：
     * 1. EMERGENCY > URGENT > NORMAL（原始级别）
     * 2. NORMAL 等待超过 30分钟 → 提升为 URGENT 级别优先级
     * 3. 同级别按 create_time 升序
     */
    List<QueuedTaskItem> sortWithAging(List<QueuedTaskItem> tasks);

    /**
     * 计算任务的有效优先级值（考虑老化）
     * 返回值越小优先级越高
     */
    int effectivePriority(QueuedTaskItem task);

    /**
     * 判断任务是否已等待超时（超过30分钟）
     */
    boolean isAgingThresholdReached(LocalDateTime createTime);

    /**
     * 获取任务的逻辑老化级别（逻辑层提升，不改DB）
     */
    String resolveEffectiveUrgencyLevel(String originalLevel, LocalDateTime createTime);
}
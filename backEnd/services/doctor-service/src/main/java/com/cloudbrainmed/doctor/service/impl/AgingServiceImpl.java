package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.doctor.mapper.MedicalOrderMapper.QueuedTaskItem;
import com.cloudbrainmed.doctor.service.AgingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 防饿死服务实现
 * ========================
 * 纯逻辑层实现，不修改数据库
 *
 * 核心规则：
 * 1. EMERGENCY > 老化URGENT > 原始URGENT > NORMAL
 * 2. NORMAL 等待超过 30分钟 → 逻辑上升为 URGENT 优先级
 * 3. 同优先级按 create_time 升序（FIFO）
 */
@Slf4j
@Service
public class AgingServiceImpl implements AgingService {

    /** 防饿死阈值：30分钟 */
    private static final long AGING_THRESHOLD_MINUTES = 30;

    @Override
    public List<QueuedTaskItem> sortWithAging(List<QueuedTaskItem> tasks) {
        return tasks.stream()
                .sorted(Comparator.comparingInt(this::effectivePriority)
                        .thenComparing(QueuedTaskItem::getCreateTime))
                .collect(Collectors.toList());
    }

    @Override
    public int effectivePriority(QueuedTaskItem task) {
        String level = task.getUrgencyLevel();
        LocalDateTime createTime = task.getCreateTime();
        boolean aged = isAgingThresholdReached(createTime);

        switch (level) {
            case "EMERGENCY":
                return 1;
            case "URGENT":
                return 2;
            case "NORMAL":
                // NORMAL 超过30分钟 → 升级为URGENT级别（返回2）
                if (aged) {
                    return 2;
                }
                return 3;
            default:
                return 4;
        }
    }

    @Override
    public boolean isAgingThresholdReached(LocalDateTime createTime) {
        if (createTime == null) {
            return false;
        }
        return Duration.between(createTime, LocalDateTime.now())
                .toMinutes() >= AGING_THRESHOLD_MINUTES;
    }

    @Override
    public String resolveEffectiveUrgencyLevel(String originalLevel, LocalDateTime createTime) {
        if ("NORMAL".equals(originalLevel) && isAgingThresholdReached(createTime)) {
            log.debug("Aging: NORMAL task created at {} promoted to URGENT level", createTime);
            return "URGENT";
        }
        return originalLevel;
    }
}

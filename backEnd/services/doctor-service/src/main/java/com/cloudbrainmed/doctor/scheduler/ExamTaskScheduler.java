package com.cloudbrainmed.doctor.scheduler;

import com.cloudbrainmed.doctor.service.SchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 检查检验任务调度器
 * 每 5 秒扫描一次 queued 队列，自动匹配医生并分配任务
 */
@Slf4j
@Component
@EnableScheduling
public class ExamTaskScheduler {

    private final SchedulerService schedulerService;

    public ExamTaskScheduler(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @Scheduled(fixedDelay = 5000)
    public void schedule() {
        try {
            int assigned = schedulerService.runScheduler();
            if (assigned > 0) {
                log.info("Scheduler assigned {} tasks", assigned);
            }
        } catch (Exception e) {
            log.error("Scheduler error: {}", e.getMessage(), e);
        }
    }
}
package com.cloudbrainmed.doctor.scheduler;

import com.cloudbrainmed.doctor.service.TaskSchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 检查检验任务调度器
 * 每 3 秒扫描一次 queued 队列，自动匹配医生并分配任务
 */
@Slf4j
@Component
@EnableScheduling
public class ExamTaskScheduler {

    private final TaskSchedulerService taskSchedulerService;

    public ExamTaskScheduler(TaskSchedulerService taskSchedulerService) {
        this.taskSchedulerService = taskSchedulerService;
    }

    @Scheduled(fixedDelay = 3000)
    public void schedule() {
        try {
            int assigned = taskSchedulerService.runScheduler();
            if (assigned > 0) {
                log.info("Scheduler assigned {} tasks", assigned);
            }
        } catch (Exception e) {
            log.error("Scheduler error: {}", e.getMessage(), e);
        }
    }
}
package com.cloudbrainmed.doctor.service;

/**
 * 调度器服务
 * 负责扫描 QUEUED 队列，自动匹配医生并分配任务
 */
public interface SchedulerService {

    /**
     * 执行一次调度扫描
     * 按优先级排序的排队任务，匹配可用医生，原子分配
     * 返回本次分配的任务数
     */
    int runScheduler();
}
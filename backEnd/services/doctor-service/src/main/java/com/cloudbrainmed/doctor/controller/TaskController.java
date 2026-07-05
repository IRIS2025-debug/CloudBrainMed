package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.doctor.service.TaskSchedulerService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 医生任务调度控制器
 * 提供医生端工作台、队列、任务操作接口
 */
@RestController
@RequestMapping("/doctor-service/task")
public class TaskController {

    private final TaskSchedulerService taskSchedulerService;

    public TaskController(TaskSchedulerService taskSchedulerService) {
        this.taskSchedulerService = taskSchedulerService;
    }

    @GetMapping("/workbench")
    public Result<?> workbench(@RequestHeader(value = "token", required = false) String token) {
        String doctorId = extractDoctorId(token);
        Integer doctorType = extractDoctorType(token);
        return Result.ok(taskSchedulerService.getDoctorWorkbench(doctorId, doctorType));
    }

    @GetMapping("/queue")
    public Result<?> queue(@RequestHeader(value = "token", required = false) String token) {
        String doctorId = extractDoctorId(token);
        Integer doctorType = extractDoctorType(token);
        return Result.ok(Map.of(
                "doctorId", doctorId,
                "tasks", taskSchedulerService.getQueue(doctorType),
                "queueCount", taskSchedulerService.getQueueCount()
        ));
    }

    @GetMapping("/detail")
    public Result<?> detail(@RequestHeader(value = "token", required = false) String token,
                            @RequestParam String orderItemId) {
        String doctorId = extractDoctorId(token);
        return Result.ok(taskSchedulerService.getTaskDetail(orderItemId, doctorId));
    }

    @PostMapping("/start")
    public Result<?> startTask(@RequestHeader(value = "token", required = false) String token,
                               @RequestBody Map<String, String> body) {
        String doctorId = extractDoctorId(token);
        taskSchedulerService.startTask(body.get("orderItemId"), doctorId);
        return Result.ok();
    }

    @PostMapping("/complete")
    public Result<?> completeTask(@RequestHeader(value = "token", required = false) String token,
                                  @RequestBody Map<String, String> body) {
        String doctorId = extractDoctorId(token);
        taskSchedulerService.completeTask(body.get("orderItemId"), doctorId);
        return Result.ok();
    }

    /**
     * 手动触发调度（调试用）
     */
    @PostMapping("/scheduler/run")
    public Result<?> runScheduler() {
        int assigned = taskSchedulerService.runScheduler();
        return Result.ok(Map.of(
                "assigned", assigned,
                "queueCount", taskSchedulerService.getQueueCount()
        ));
    }

    private String extractDoctorId(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("未登录，请先登录");
        }
        try {
            Integer roleType = DoctorJwtUtil.getRoleType(token);
            if (!Integer.valueOf(2).equals(roleType)) {
                throw new BusinessException("仅医生可访问");
            }
            Integer doctorType = DoctorJwtUtil.getDoctorType(token);
            if (doctorType == null || Integer.valueOf(1).equals(doctorType)) {
                throw new BusinessException("仅检查/检验医生可访问此功能");
            }
            return DoctorJwtUtil.getUserId(token);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("医生登录凭证无效");
        }
    }

    private Integer extractDoctorType(String token) {
        return DoctorJwtUtil.getDoctorType(token);
    }
}
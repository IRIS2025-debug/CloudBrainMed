package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.cloudbrainmed.doctor.dto.MedicalReportSubmitRequest;
import com.cloudbrainmed.doctor.service.TaskSchedulerService;
import com.cloudbrainmed.doctor.vo.DoctorTaskDetailVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 检查/检验医生任务处理接口。
 */
@RestController
@RequestMapping("/doctor-service/task")
public class TaskController {

    private final TaskSchedulerService taskSchedulerService;

    public TaskController(TaskSchedulerService taskSchedulerService) {
        this.taskSchedulerService = taskSchedulerService;
    }

    @GetMapping("/detail")
    public Result<?> detail(@RequestHeader(value = "token", required = false) String token,
                            @RequestParam String orderItemId) {
        DoctorContext doctor = extractDoctorContext(token);
        DoctorTaskDetailVo detail = taskSchedulerService.getTaskDetail(
                orderItemId, doctor.doctorId(), doctor.doctorType());
        requireDoctorTypeForItem(detail.getItemCategory(), doctor.doctorType());
        return Result.ok(detail);
    }

    @PostMapping("/start")
    public Result<?> startTask(@RequestHeader(value = "token", required = false) String token,
                               @RequestBody Map<String, String> body) {
        DoctorContext doctor = extractDoctorContext(token);
        taskSchedulerService.startTask(
                body.get("orderItemId"), doctor.doctorId(), doctor.doctorType());
        return Result.ok();
    }

    @PostMapping("/complete")
    public Result<?> completeTask(@RequestHeader(value = "token", required = false) String token,
                                  @RequestBody Map<String, String> body) {
        DoctorContext doctor = extractDoctorContext(token);
        taskSchedulerService.completeTask(
                body.get("orderItemId"), doctor.doctorId(), doctor.doctorType());
        return Result.ok();
    }

    @PostMapping("/report")
    public Result<?> submitReport(
            @RequestHeader(value = "token", required = false) String token,
            @Valid @RequestBody MedicalReportSubmitRequest request) {
        DoctorContext doctor = extractDoctorContext(token);
        return Result.ok(taskSchedulerService.submitReport(
                request, doctor.doctorId(), doctor.doctorType()));
    }

    private DoctorContext extractDoctorContext(String token) {
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
            return new DoctorContext(DoctorJwtUtil.getUserId(token), doctorType);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("医生登录凭证无效");
        }
    }

    private record DoctorContext(String doctorId, Integer doctorType) {
    }

    private void requireDoctorTypeForItem(String itemCategory, Integer doctorType) {
        boolean allowed = ("EXAM".equals(itemCategory) && Integer.valueOf(2).equals(doctorType))
                || ("LAB".equals(itemCategory) && Integer.valueOf(3).equals(doctorType));
        if (!allowed) {
            throw new BusinessException("无权处理该检查/检验任务");
        }
    }
}

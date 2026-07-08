package com.cloudbrainmed.admin.controller;

import com.cloudbrainmed.admin.dto.ScheduleQueryDto;
import com.cloudbrainmed.admin.dto.ScheduleSaveDto;
import com.cloudbrainmed.admin.dto.ScheduleUpdateDto;
import com.cloudbrainmed.admin.entity.DoctorSchedule;
import com.cloudbrainmed.admin.service.ScheduleManageService;
import com.cloudbrainmed.admin.support.AdminAuthHelper;
import com.cloudbrainmed.common.result.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin-service/schedule")
@RequiredArgsConstructor
public class ScheduleManageController {

    private final ScheduleManageService scheduleService;
    private final AdminAuthHelper adminAuthHelper;

    // ========== 前端接口 ==========

    /**
     * 分页查询排班列表（前端用）
     */
    @PostMapping("/list")
    public Result<Page<DoctorSchedule>> queryList(
            @RequestHeader(value = "token", required = false) String token,
            @RequestBody ScheduleQueryDto dto) {
        adminAuthHelper.requireAdmin(token);
        return Result.success(scheduleService.queryScheduleList(dto));
    }

    /**
     * 获取医生某周的排班（前端用）
     */
    @GetMapping("/weekly/{doctorId}")
    public Result<Map<String, List<DoctorSchedule>>> getWeeklySchedule(
            @RequestHeader(value = "token", required = false) String token,
            @PathVariable String doctorId,
            @RequestParam(required = false) String weekStart) {
        adminAuthHelper.requireAdmin(token);

        LocalDate start = weekStart != null ? LocalDate.parse(weekStart) : LocalDate.now();
        LocalDate monday = start.with(java.time.DayOfWeek.MONDAY);
        return Result.success(scheduleService.getWeeklySchedule(doctorId, monday));
    }

    /**
     * 获取所有医生本周排班（前端用）
     */
    @GetMapping("/weekly/all")
    public Result<Map<String, Map<String, List<DoctorSchedule>>>> getAllWeeklySchedule(
            @RequestHeader(value = "token", required = false) String token,
            @RequestParam(required = false) String weekStart) {
        adminAuthHelper.requireAdmin(token);

        LocalDate start = weekStart != null ? LocalDate.parse(weekStart) : LocalDate.now();
        LocalDate monday = start.with(java.time.DayOfWeek.MONDAY);
        return Result.success(scheduleService.getAllWeeklySchedule(monday));
    }

    /**
     * 获取排班详情（前端用）
     */
    @GetMapping("/detail/{scheduleId}")
    public Result<DoctorSchedule> getDetail(
            @RequestHeader(value = "token", required = false) String token,
            @PathVariable String scheduleId) {
        adminAuthHelper.requireAdmin(token);
        return Result.success(scheduleService.getScheduleDetail(scheduleId));
    }

    /**
     * 创建排班（前端用）
     */
    @PostMapping("/create")
    public Result<DoctorSchedule> create(
            @RequestHeader(value = "token", required = false) String token,
            @RequestBody ScheduleSaveDto dto) {
        adminAuthHelper.requireAdmin(token);
        return Result.success(scheduleService.createSchedule(dto));
    }

    /**
     * 更新排班（前端用）
     */
    @PutMapping("/update")
    public Result<DoctorSchedule> update(
            @RequestHeader(value = "token", required = false) String token,
            @RequestBody ScheduleUpdateDto dto) {
        adminAuthHelper.requireAdmin(token);
        return Result.success(scheduleService.updateSchedule(dto));
    }

    /**
     * 删除排班（前端用）
     */
    @DeleteMapping("/delete/{scheduleId}")
    public Result<Boolean> delete(
            @RequestHeader(value = "token", required = false) String token,
            @PathVariable String scheduleId) {
        adminAuthHelper.requireAdmin(token);
        return Result.success(scheduleService.deleteSchedule(scheduleId));
    }

    // ========== AI服务调用接口（共用） ==========

    /**
     * 获取医生排班（供AI智能排班调用）
     * 前端也可以调用这个接口，只是参数格式不同
     */
    @GetMapping("/ai/doctor/{doctorId}")
    public Result<List<DoctorSchedule>> getSchedulesForAI(
            @RequestHeader(value = "token", required = false) String token,
            @PathVariable String doctorId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        adminAuthHelper.requireAdmin(token);

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        return Result.success(scheduleService.getDoctorSchedulesForAI(doctorId, start, end));
    }

    /**
     * 批量创建排班（供AI生成后调用）
     * 前端如果有批量导入需求也可以使用
     */
    @PostMapping("/ai/batch-create")
    public Result<List<DoctorSchedule>> batchCreate(
            @RequestHeader(value = "token", required = false) String token,
            @RequestBody List<ScheduleSaveDto> dtoList) {
        adminAuthHelper.requireAdmin(token);
        return Result.success(scheduleService.batchCreateSchedules(dtoList));
    }

    /**
     * 检查排班冲突（供AI调用）
     */
    @PostMapping("/ai/check-conflict")
    public Result<Boolean> checkConflict(
            @RequestHeader(value = "token", required = false) String token,
            @RequestBody DoctorSchedule schedule) {
        adminAuthHelper.requireAdmin(token);
        return Result.success(scheduleService.checkScheduleConflict(schedule));
    }

    /**
     * 启用排班（将停用的排班重新启用）
     */
    @PutMapping("/enable/{scheduleId}")
    public Result<Boolean> enable(
            @RequestHeader(value = "token", required = false) String token,
            @PathVariable String scheduleId) {
        adminAuthHelper.requireAdmin(token);
        return Result.success(scheduleService.enableSchedule(scheduleId));
    }
}

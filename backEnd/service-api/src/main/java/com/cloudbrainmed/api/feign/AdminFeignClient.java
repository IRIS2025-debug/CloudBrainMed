package com.cloudbrainmed.api.feign;

import com.cloudbrainmed.api.fallback.AdminFeignFallback;
import com.cloudbrainmed.admin.dto.ScheduleSaveDto;
import com.cloudbrainmed.admin.entity.DoctorSchedule;
import com.cloudbrainmed.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "admin-service", fallback = AdminFeignFallback.class)
public interface AdminFeignClient {

    /**
     * 获取医生排班（AI智能排班用）
     * 路径和ScheduleManageController中的方法对应
     */
    @GetMapping("/api/admin/schedule/ai/doctor/{doctorId}")
    Result<List<DoctorSchedule>> getDoctorSchedulesForAI(
            @PathVariable("doctorId") String doctorId,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate
    );

    /**
     * 批量创建排班（AI生成后调用）
     */
    @PostMapping("/api/admin/schedule/ai/batch-create")
    Result<List<DoctorSchedule>> batchCreateSchedules(@RequestBody List<ScheduleSaveDto> dtoList);

    /**
     * 检查排班冲突
     */
    @PostMapping("/api/admin/schedule/ai/check-conflict")
    Result<Boolean> checkConflict(@RequestBody DoctorSchedule schedule);
}
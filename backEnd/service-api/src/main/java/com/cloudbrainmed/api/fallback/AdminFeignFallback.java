package com.cloudbrainmed.api.fallback;

import com.cloudbrainmed.admin.dto.ScheduleBatchCreateResponse;
import com.cloudbrainmed.admin.dto.ScheduleConflictResult;
import com.cloudbrainmed.admin.dto.ScheduleSaveDto;
import com.cloudbrainmed.admin.entity.DoctorSchedule;
import com.cloudbrainmed.api.feign.AdminFeignClient;
import com.cloudbrainmed.common.result.Result;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdminFeignFallback implements AdminFeignClient {

    @Override
    public Result<List<DoctorSchedule>> getDoctorSchedulesForAI(String doctorId, String startDate, String endDate) {
        return Result.error("管理员服务不可用，获取医生排班失败");
    }

    @Override
    public Result<List<DoctorSchedule>> getRoomUsageForAI(List<String> rooms, String startDate, String endDate) {
        return Result.error("管理员服务不可用，获取诊室占用失败");
    }

    @Override
    public Result<ScheduleBatchCreateResponse> batchCreateSchedules(List<ScheduleSaveDto> dtoList) {
        return Result.error("管理员服务不可用，批量创建排班失败");
    }

    @Override
    public Result<ScheduleConflictResult> checkConflict(DoctorSchedule schedule) {
        return Result.error("管理员服务不可用，检查排班冲突失败");
    }
}

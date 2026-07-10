package com.cloudbrainmed.admin.service;

import com.cloudbrainmed.admin.dto.ScheduleQueryDto;
import com.cloudbrainmed.admin.dto.ScheduleBatchCreateResponse;
import com.cloudbrainmed.admin.dto.ScheduleConflictResult;
import com.cloudbrainmed.admin.dto.ScheduleSaveDto;
import com.cloudbrainmed.admin.dto.ScheduleUpdateDto;
import com.cloudbrainmed.admin.entity.DoctorSchedule;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ScheduleManageService {

    /**
     * 分页查询排班列表
     */
    Page<DoctorSchedule> queryScheduleList(ScheduleQueryDto dto);

    /**
     * 获取医生某周的排班
     */
    Map<String, List<DoctorSchedule>> getWeeklySchedule(String doctorId, LocalDate weekStart);

    /**
     * 获取所有医生某周的排班（总览）
     */
    Map<String, Map<String, List<DoctorSchedule>>> getAllWeeklySchedule(LocalDate weekStart);

    /**
     * 获取排班详情
     */
    DoctorSchedule getScheduleDetail(String scheduleId);

    /**
     * 创建排班
     */
    DoctorSchedule createSchedule(ScheduleSaveDto dto);

    /**
     * 更新排班
     */
    DoctorSchedule updateSchedule(ScheduleUpdateDto dto);

    /**
     * 删除排班（软删除，改为停用状态）
     */
    boolean deleteSchedule(String scheduleId);

    /**
     * 启用排班（将停用的排班重新启用）
     */
    boolean enableSchedule(String scheduleId);

    /**
     * 批量生成排班（AI调用）
     */
    ScheduleBatchCreateResponse batchCreateSchedules(List<ScheduleSaveDto> dtoList);

    /**
     * 获取医生排班（供AI服务调用）
     */
    List<DoctorSchedule> getDoctorSchedulesForAI(String doctorId, LocalDate startDate, LocalDate endDate);

    /**
     * 获取诊室占用（供AI服务生成排班草稿时避开已占用诊室）
     */
    List<DoctorSchedule> getRoomUsagesForAI(List<String> rooms, LocalDate startDate, LocalDate endDate);

    /**
     * 检查排班冲突
     */
    boolean checkScheduleConflict(DoctorSchedule schedule);

    /**
     * 检查排班冲突并返回冲突类型和原因
     */
    ScheduleConflictResult checkScheduleConflictDetail(DoctorSchedule schedule);
}

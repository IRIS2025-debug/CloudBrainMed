package com.cloudbrainmed.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudbrainmed.admin.dto.ScheduleQueryDto;
import com.cloudbrainmed.admin.dto.ScheduleSaveDto;
import com.cloudbrainmed.admin.dto.ScheduleUpdateDto;
import com.cloudbrainmed.admin.entity.DoctorManage;
import com.cloudbrainmed.admin.entity.DoctorSchedule;
import com.cloudbrainmed.admin.mapper.DoctorManageMapper;
import com.cloudbrainmed.admin.mapper.ScheduleMapper;
import com.cloudbrainmed.admin.service.ScheduleManageService;
import com.cloudbrainmed.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleManageServiceImpl implements ScheduleManageService {

    private final ScheduleMapper scheduleMapper;
    private final DoctorManageMapper doctorManageMapper;

    @Override
    public Page<DoctorSchedule> queryScheduleList(ScheduleQueryDto dto) {
        LambdaQueryWrapper<DoctorSchedule> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(dto.getDoctorId())) {
            wrapper.eq(DoctorSchedule::getDoctorId, dto.getDoctorId());
        }
        if (StringUtils.hasText(dto.getDeptId())) {
            wrapper.eq(DoctorSchedule::getDeptId, dto.getDeptId());
        }
        if (dto.getStartDate() != null) {
            wrapper.ge(DoctorSchedule::getWorkDate, dto.getStartDate());
        }
        if (dto.getEndDate() != null) {
            wrapper.le(DoctorSchedule::getWorkDate, dto.getEndDate());
        }
        wrapper.eq(DoctorSchedule::getScheduleStatus, "PUBLISHED");
        wrapper.orderByAsc(DoctorSchedule::getWorkDate)
                .orderByAsc(DoctorSchedule::getStartTime);

        return scheduleMapper.selectPage(new Page<>(dto.getPage(), dto.getLimit()), wrapper);
    }

    @Override
    public Map<String, List<DoctorSchedule>> getWeeklySchedule(String doctorId, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        List<DoctorSchedule> schedules = scheduleMapper.selectByDoctorIdAndDateRange(
                doctorId, weekStart, weekEnd);

        return schedules.stream()
                .collect(Collectors.groupingBy(s -> s.getWorkDate().toString()));
    }

    @Override
    public Map<String, Map<String, List<DoctorSchedule>>> getAllWeeklySchedule(LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);

        // 1. 获取所有在职医生的ID列表
        Set<String> activeDoctorIds = getActiveDoctorIds();

        // 2. 查询排班数据
        List<DoctorSchedule> schedules = scheduleMapper.selectByDateRange(weekStart, weekEnd);

        // 3. 过滤：只保留在职医生的排班
        List<DoctorSchedule> filteredSchedules = schedules.stream()
                .filter(s -> activeDoctorIds.contains(s.getDoctorId()))
                .collect(Collectors.toList());

        // 4. 按医生分组，再按日期分组
        return filteredSchedules.stream()
                .collect(Collectors.groupingBy(
                        DoctorSchedule::getDoctorId,
                        LinkedHashMap::new,
                        Collectors.groupingBy(s -> s.getWorkDate().toString())
                ));
    }

    @Override
    public DoctorSchedule getScheduleDetail(String scheduleId) {
        DoctorSchedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            throw new BusinessException("排班不存在");
        }
        return schedule;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DoctorSchedule createSchedule(ScheduleSaveDto dto) {
        // 验证医生是否在职
        if (!isDoctorActive(dto.getDoctorId())) {
            throw new BusinessException("医生不存在或已停职，无法创建排班");
        }

        // 校验时间：开始时间必须小于结束时间
        if (dto.getStartTime() != null && dto.getEndTime() != null) {
            if (dto.getStartTime().isAfter(dto.getEndTime()) || dto.getStartTime().equals(dto.getEndTime())) {
                throw new BusinessException("开始时间必须早于结束时间");
            }
        }

        // 检查冲突
        int conflictCount = scheduleMapper.checkConflict(
                dto.getDoctorId(),
                dto.getWorkDate(),
                dto.getStartTime(),
                dto.getEndTime(),
                null
        );
        if (conflictCount > 0) {
            throw new BusinessException("该时段已有排班，请调整时间");
        }

        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setScheduleId(generateScheduleId());
        schedule.setDoctorId(dto.getDoctorId());
        schedule.setDoctorName(dto.getDoctorName());
        schedule.setDeptId(dto.getDeptId());
        schedule.setWorkDate(dto.getWorkDate());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setMaxNum(dto.getMaxNum());
        schedule.setRemainNum(dto.getMaxNum());
        schedule.setPrice(dto.getPrice());
        schedule.setRoom(dto.getRoom());
        schedule.setStatus(1);
        schedule.setSourceType("MANUAL");
        schedule.setScheduleStatus("PUBLISHED");
        schedule.setCreateTime(OffsetDateTime.now(ZoneOffset.ofHours(8)));

        scheduleMapper.insert(schedule);
        return schedule;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public DoctorSchedule updateSchedule(ScheduleUpdateDto dto) {
        DoctorSchedule existing = scheduleMapper.selectById(dto.getScheduleId());
        if (existing == null) {
            throw new BusinessException("排班不存在");
        }

        // 如果更换医生，验证新医生是否在职
        if (dto.getDoctorId() != null && !dto.getDoctorId().equals(existing.getDoctorId())) {
            if (!isDoctorActive(dto.getDoctorId())) {
                throw new BusinessException("医生不存在或已停职");
            }
        }

        // 只能修改当天及之后的排班
        LocalDate today = LocalDate.now(ZoneOffset.ofHours(8));
        LocalDate workDate = dto.getWorkDate() != null ? dto.getWorkDate() : existing.getWorkDate();
        if (workDate.isBefore(today)) {
            throw new BusinessException("不能修改过去的排班（" + workDate + "），只能修改今天及之后的排班");
        }

        // 校验时间：开始时间必须小于结束时间
        LocalTime startTime = dto.getStartTime() != null ? dto.getStartTime() : existing.getStartTime();
        LocalTime endTime = dto.getEndTime() != null ? dto.getEndTime() : existing.getEndTime();
        if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
            throw new BusinessException("开始时间必须早于结束时间");
        }

        // 检查冲突（排除自己）
        String doctorId = dto.getDoctorId() != null ? dto.getDoctorId() : existing.getDoctorId();
        int conflictCount = scheduleMapper.checkConflict(
                doctorId,
                workDate,
                startTime,
                endTime,
                dto.getScheduleId()
        );
        if (conflictCount > 0) {
            throw new BusinessException("该时段已有排班，请调整时间");
        }

        // 更新字段
        if (dto.getDoctorId() != null) existing.setDoctorId(dto.getDoctorId());
        if (dto.getDoctorName() != null) existing.setDoctorName(dto.getDoctorName());
        if (dto.getDeptId() != null) existing.setDeptId(dto.getDeptId());
        if (dto.getWorkDate() != null) existing.setWorkDate(dto.getWorkDate());
        if (dto.getStartTime() != null) existing.setStartTime(dto.getStartTime());
        if (dto.getEndTime() != null) existing.setEndTime(dto.getEndTime());
        if (dto.getMaxNum() != null) {
            int diff = dto.getMaxNum() - existing.getMaxNum();
            existing.setMaxNum(dto.getMaxNum());
            existing.setRemainNum(Math.max(0, existing.getRemainNum() + diff));
        }
        if (dto.getPrice() != null) existing.setPrice(dto.getPrice());
        if (dto.getRoom() != null) existing.setRoom(dto.getRoom());
        if (dto.getStatus() != null) existing.setStatus(dto.getStatus());

        existing.setUpdateTime(OffsetDateTime.now(ZoneOffset.ofHours(8)));

        scheduleMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSchedule(String scheduleId) {
        DoctorSchedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            throw new BusinessException("排班不存在");
        }

        // 只能删除当天及之后的排班
        LocalDate today = LocalDate.now(ZoneOffset.ofHours(8));
        if (schedule.getWorkDate().isBefore(today)) {
            throw new BusinessException("不能删除过去的排班（" + schedule.getWorkDate() + "），只能删除今天及之后的排班");
        }

        return scheduleMapper.updateStatus(scheduleId, 0) > 0;
    }

    @Override
    public List<DoctorSchedule> getDoctorSchedulesForAI(String doctorId, LocalDate startDate, LocalDate endDate) {
        // AI调用时也需要验证医生是否在职
        if (!isDoctorActive(doctorId)) {
            throw new BusinessException("医生不存在或已停职");
        }
        return scheduleMapper.selectByDoctorIdAndDateRange(doctorId, startDate, endDate);
    }

    @Override
    public boolean checkScheduleConflict(DoctorSchedule schedule) {
        int count = scheduleMapper.checkConflict(
                schedule.getDoctorId(),
                schedule.getWorkDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getScheduleId()
        );
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean enableSchedule(String scheduleId) {
        DoctorSchedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            throw new BusinessException("排班不存在");
        }

        // 验证医生是否在职
        if (!isDoctorActive(schedule.getDoctorId())) {
            throw new BusinessException("该排班的医生已停职，无法启用");
        }

        // 只能启用当天及之后的排班
        LocalDate today = LocalDate.now(ZoneOffset.ofHours(8));
        if (schedule.getWorkDate().isBefore(today)) {
            throw new BusinessException("不能启用过去的排班（" + schedule.getWorkDate() + "），只能启用今天及之后的排班");
        }

        if (schedule.getStatus() == 1) {
            throw new BusinessException("排班已经是启用状态");
        }

        int conflictCount = scheduleMapper.checkConflict(
                schedule.getDoctorId(),
                schedule.getWorkDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getScheduleId()
        );
        if (conflictCount > 0) {
            throw new BusinessException("该时段已有启用的排班，无法启用");
        }

        return scheduleMapper.enableSchedule(scheduleId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<DoctorSchedule> batchCreateSchedules(List<ScheduleSaveDto> dtoList) {
        List<DoctorSchedule> result = new ArrayList<>();
        for (ScheduleSaveDto dto : dtoList) {
            // 验证医生是否在职
            if (!isDoctorActive(dto.getDoctorId())) {
                continue;  // 跳过停职医生的排班
            }

            if (dto.getStartTime() != null && dto.getEndTime() != null) {
                if (dto.getStartTime().isAfter(dto.getEndTime()) || dto.getStartTime().equals(dto.getEndTime())) {
                    continue;
                }
            }

            int conflictCount = scheduleMapper.checkConflict(
                    dto.getDoctorId(),
                    dto.getWorkDate(),
                    dto.getStartTime(),
                    dto.getEndTime(),
                    null
            );
            if (conflictCount == 0) {
                DoctorSchedule schedule = new DoctorSchedule();
                schedule.setScheduleId(generateScheduleId());
                schedule.setDoctorId(dto.getDoctorId());
                schedule.setDoctorName(dto.getDoctorName());
                schedule.setDeptId(dto.getDeptId());
                schedule.setWorkDate(dto.getWorkDate());
                schedule.setStartTime(dto.getStartTime());
                schedule.setEndTime(dto.getEndTime());
                schedule.setMaxNum(dto.getMaxNum());
                schedule.setRemainNum(dto.getMaxNum());
                schedule.setPrice(dto.getPrice());
                schedule.setRoom(dto.getRoom());
                schedule.setStatus(1);
                schedule.setSourceType("AI_GENERATED");
                schedule.setScheduleStatus("PUBLISHED");
                schedule.setCreateTime(OffsetDateTime.now(ZoneOffset.ofHours(8)));

                scheduleMapper.insert(schedule);
                result.add(schedule);
            }
        }
        return result;
    }

    // ===== 私有辅助方法 =====

    /**
     * 获取所有在职医生的ID集合
     */
    private Set<String> getActiveDoctorIds() {
        LambdaQueryWrapper<DoctorManage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DoctorManage::getStatus, 1)
                .eq(DoctorManage::getIsDeleted, 0)
                .select(DoctorManage::getDoctorId);
        List<DoctorManage> doctors = doctorManageMapper.selectList(wrapper);
        return doctors.stream()
                .map(DoctorManage::getDoctorId)
                .collect(Collectors.toSet());
    }

    /**
     * 判断医生是否在职
     */
    private boolean isDoctorActive(String doctorId) {
        if (doctorId == null) return false;
        LambdaQueryWrapper<DoctorManage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DoctorManage::getDoctorId, doctorId)
                .eq(DoctorManage::getStatus, 1)
                .eq(DoctorManage::getIsDeleted, 0);
        return doctorManageMapper.selectCount(wrapper) > 0;
    }

    private String generateScheduleId() {
        return "SCH" + System.currentTimeMillis() + String.format("%04d", new Random().nextInt(10000));
    }
}
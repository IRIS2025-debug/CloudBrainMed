package com.cloudbrainmed.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudbrainmed.admin.dto.ScheduleBatchCreateResponse;
import com.cloudbrainmed.admin.dto.ScheduleConflictResult;
import com.cloudbrainmed.admin.dto.SchedulePublishFailure;
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

        ScheduleConflictResult conflict = checkScheduleConflictDetail(toDoctorSchedule(dto, null));
        if (conflict.isConflict()) {
            throw new BusinessException(conflict.getConflictReason());
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

        String doctorId = dto.getDoctorId() != null ? dto.getDoctorId() : existing.getDoctorId();
        DoctorSchedule candidate = new DoctorSchedule();
        candidate.setScheduleId(dto.getScheduleId());
        candidate.setDoctorId(doctorId);
        candidate.setDoctorName(dto.getDoctorName() != null ? dto.getDoctorName() : existing.getDoctorName());
        candidate.setDeptId(dto.getDeptId() != null ? dto.getDeptId() : existing.getDeptId());
        candidate.setWorkDate(workDate);
        candidate.setStartTime(startTime);
        candidate.setEndTime(endTime);
        candidate.setRoom(dto.getRoom() != null ? dto.getRoom() : existing.getRoom());
        ScheduleConflictResult conflict = checkScheduleConflictDetail(candidate);
        if (conflict.isConflict()) {
            throw new BusinessException(conflict.getConflictReason());
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
    public List<DoctorSchedule> getRoomUsagesForAI(List<String> rooms, LocalDate startDate, LocalDate endDate) {
        Set<String> roomSet = rooms == null ? Set.of() : rooms.stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (roomSet.isEmpty()) {
            return List.of();
        }
        return scheduleMapper.selectByDateRange(startDate, endDate).stream()
                .filter(schedule -> Integer.valueOf(1).equals(schedule.getStatus()))
                .filter(schedule -> StringUtils.hasText(schedule.getRoom()))
                .filter(schedule -> roomSet.contains(schedule.getRoom()))
                .toList();
    }

    @Override
    public boolean checkScheduleConflict(DoctorSchedule schedule) {
        return checkScheduleConflictDetail(schedule).isConflict();
    }

    @Override
    public ScheduleConflictResult checkScheduleConflictDetail(DoctorSchedule schedule) {
        if (schedule == null
                || schedule.getDoctorId() == null
                || schedule.getWorkDate() == null
                || schedule.getStartTime() == null
                || schedule.getEndTime() == null) {
            return ScheduleConflictResult.of(
                    ScheduleConflictResult.DOCTOR_TIME,
                    "排班医生、日期或时间不完整，无法检查冲突",
                    null,
                    schedule == null ? null : schedule.getDoctorId(),
                    schedule == null ? null : schedule.getDoctorName(),
                    schedule == null ? null : schedule.getRoom());
        }

        DoctorSchedule doctorConflict = scheduleMapper.findDoctorTimeConflict(
                schedule.getDoctorId(),
                schedule.getWorkDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getScheduleId()
        );
        if (doctorConflict != null) {
            return ScheduleConflictResult.of(
                    ScheduleConflictResult.DOCTOR_TIME,
                    "医生 " + valueOrDefault(schedule.getDoctorName(), doctorConflict.getDoctorName())
                            + " 在该时段已有排班",
                    doctorConflict.getScheduleId(),
                    doctorConflict.getDoctorId(),
                    doctorConflict.getDoctorName(),
                    doctorConflict.getRoom());
        }

        if (StringUtils.hasText(schedule.getRoom())) {
            DoctorSchedule roomConflict = scheduleMapper.findRoomTimeConflict(
                    schedule.getRoom(),
                    schedule.getWorkDate(),
                    schedule.getStartTime(),
                    schedule.getEndTime(),
                    schedule.getScheduleId()
            );
            if (roomConflict != null) {
                return ScheduleConflictResult.of(
                        ScheduleConflictResult.ROOM_TIME,
                        "诊室 " + schedule.getRoom() + " 在该时段已被 "
                                + valueOrDefault(roomConflict.getDoctorName(), "其他医生")
                                + " 占用",
                        roomConflict.getScheduleId(),
                        roomConflict.getDoctorId(),
                        roomConflict.getDoctorName(),
                        roomConflict.getRoom());
            }
        }
        return ScheduleConflictResult.none();
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

        ScheduleConflictResult conflict = checkScheduleConflictDetail(schedule);
        if (conflict.isConflict()) {
            throw new BusinessException(conflict.getConflictReason());
        }

        return scheduleMapper.enableSchedule(scheduleId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleBatchCreateResponse batchCreateSchedules(List<ScheduleSaveDto> dtoList) {
        ScheduleBatchCreateResponse response = new ScheduleBatchCreateResponse();
        List<ScheduleSaveDto> items = dtoList == null ? List.of() : dtoList;
        response.setSubmittedCount(items.size());
        List<DoctorSchedule> acceptedSchedules = new ArrayList<>();

        for (int index = 0; index < items.size(); index++) {
            ScheduleSaveDto dto = items.get(index);
            if (dto == null) {
                response.getFailedItems().add(buildFailure(
                        index, null, "排班数据为空", ScheduleConflictResult.NONE));
                continue;
            }
            if (!isDoctorActive(dto.getDoctorId())) {
                response.getFailedItems().add(buildFailure(
                        index, dto, "医生不存在或已停职，无法创建排班", ScheduleConflictResult.NONE));
                continue;
            }
            if (!isValidTimeRange(dto.getStartTime(), dto.getEndTime())) {
                response.getFailedItems().add(buildFailure(
                        index, dto, "开始时间必须早于结束时间", ScheduleConflictResult.NONE));
                continue;
            }

            DoctorSchedule candidate = toDoctorSchedule(dto, null);
            ScheduleConflictResult persistedConflict = checkScheduleConflictDetail(candidate);
            if (persistedConflict.isConflict()) {
                response.getFailedItems().add(buildFailure(index, dto, persistedConflict));
                continue;
            }

            ScheduleConflictResult batchConflict = checkBatchConflict(candidate, acceptedSchedules);
            if (batchConflict.isConflict()) {
                response.getFailedItems().add(buildFailure(index, dto, batchConflict));
                continue;
            }

            DoctorSchedule schedule = toDoctorSchedule(dto, generateScheduleId());
            schedule.setRemainNum(dto.getMaxNum());
            schedule.setStatus(1);
            schedule.setSourceType("AI_GENERATED");
            schedule.setScheduleStatus("PUBLISHED");
            schedule.setCreateTime(OffsetDateTime.now(ZoneOffset.ofHours(8)));

            scheduleMapper.insert(schedule);
            response.getCreatedSchedules().add(schedule);
            acceptedSchedules.add(schedule);
        }
        response.setCreatedCount(response.getCreatedSchedules().size());
        if (!response.getFailedItems().isEmpty()) {
            response.getWarnings().add("部分排班因医生时间冲突、诊室占用或参数无效未创建");
        }
        return response;
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

    private DoctorSchedule toDoctorSchedule(ScheduleSaveDto dto, String scheduleId) {
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setScheduleId(scheduleId);
        schedule.setDoctorId(dto.getDoctorId());
        schedule.setDoctorName(dto.getDoctorName());
        schedule.setDeptId(dto.getDeptId());
        schedule.setWorkDate(dto.getWorkDate());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setMaxNum(dto.getMaxNum());
        schedule.setPrice(dto.getPrice());
        schedule.setRoom(dto.getRoom());
        return schedule;
    }

    private boolean isValidTimeRange(LocalTime startTime, LocalTime endTime) {
        return startTime != null && endTime != null && startTime.isBefore(endTime);
    }

    private ScheduleConflictResult checkBatchConflict(
            DoctorSchedule candidate,
            List<DoctorSchedule> acceptedSchedules) {
        for (DoctorSchedule accepted : acceptedSchedules) {
            if (!Objects.equals(candidate.getWorkDate(), accepted.getWorkDate())
                    || !timeOverlaps(candidate, accepted)) {
                continue;
            }
            if (Objects.equals(candidate.getDoctorId(), accepted.getDoctorId())) {
                return ScheduleConflictResult.of(
                        ScheduleConflictResult.BATCH_DOCTOR_TIME,
                        "本批次中医生 " + valueOrDefault(candidate.getDoctorName(), accepted.getDoctorName())
                                + " 在该时段已有排班",
                        accepted.getScheduleId(),
                        accepted.getDoctorId(),
                        accepted.getDoctorName(),
                        accepted.getRoom());
            }
            if (StringUtils.hasText(candidate.getRoom())
                    && Objects.equals(candidate.getRoom(), accepted.getRoom())) {
                return ScheduleConflictResult.of(
                        ScheduleConflictResult.BATCH_ROOM_TIME,
                        "本批次中诊室 " + candidate.getRoom() + " 在该时段已被 "
                                + valueOrDefault(accepted.getDoctorName(), "其他医生")
                                + " 占用",
                        accepted.getScheduleId(),
                        accepted.getDoctorId(),
                        accepted.getDoctorName(),
                        accepted.getRoom());
            }
        }
        return ScheduleConflictResult.none();
    }

    private boolean timeOverlaps(DoctorSchedule left, DoctorSchedule right) {
        return left.getStartTime() != null
                && left.getEndTime() != null
                && right.getStartTime() != null
                && right.getEndTime() != null
                && left.getStartTime().isBefore(right.getEndTime())
                && left.getEndTime().isAfter(right.getStartTime());
    }

    private SchedulePublishFailure buildFailure(
            int index,
            ScheduleSaveDto dto,
            ScheduleConflictResult conflict) {
        return buildFailure(index, dto,
                conflict.getConflictReason(), conflict.getConflictType());
    }

    private SchedulePublishFailure buildFailure(
            int index,
            ScheduleSaveDto dto,
            String reason,
            String conflictType) {
        SchedulePublishFailure failure = new SchedulePublishFailure();
        failure.setIndex(index);
        if (dto != null) {
            failure.setDoctorId(dto.getDoctorId());
            failure.setDoctorName(dto.getDoctorName());
            failure.setWorkDate(dto.getWorkDate());
            failure.setStartTime(dto.getStartTime());
            failure.setEndTime(dto.getEndTime());
            failure.setRoom(dto.getRoom());
        }
        failure.setReason(reason);
        failure.setConflictType(conflictType);
        return failure;
    }

    private String valueOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}

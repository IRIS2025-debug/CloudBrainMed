package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.doctor.dto.DoctorWorkRuleRequest;
import com.cloudbrainmed.doctor.entity.Doctor;
import com.cloudbrainmed.doctor.entity.DoctorWorkRule;
import com.cloudbrainmed.doctor.mapper.DoctorMapper;
import com.cloudbrainmed.doctor.mapper.DoctorWorkRuleMapper;
import com.cloudbrainmed.doctor.service.DoctorWorkRuleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DoctorWorkRuleServiceImpl implements DoctorWorkRuleService {

    private final DoctorMapper doctorMapper;
    private final DoctorWorkRuleMapper workRuleMapper;

    public DoctorWorkRuleServiceImpl(
            DoctorMapper doctorMapper,
            DoctorWorkRuleMapper workRuleMapper) {
        this.doctorMapper = doctorMapper;
        this.workRuleMapper = workRuleMapper;
    }

    @Override
    public List<DoctorWorkRule> list(String doctorId) {
        requireEnabledDoctor(doctorId);
        return workRuleMapper.selectByDoctorId(doctorId);
    }

    @Override
    @Transactional
    public DoctorWorkRule create(
            DoctorWorkRuleRequest request, String doctorId) {
        Doctor doctor = requireEnabledDoctor(doctorId);
        DoctorWorkRule rule = toEntity(
                request, doctorId, doctor.getDepartmentId());
        rule.setRuleId(newId("DWR"));
        rule.setCreateTime(LocalDateTime.now());
        validate(rule);
        workRuleMapper.insert(rule);
        return rule;
    }

    @Override
    @Transactional
    public DoctorWorkRule update(
            String ruleId,
            DoctorWorkRuleRequest request,
            String doctorId) {
        DoctorWorkRule existing =
                workRuleMapper.selectOwned(ruleId, doctorId);
        if (existing == null) {
            throw new BusinessException("工作时间规则不存在");
        }
        DoctorWorkRule rule = toEntity(
                request, doctorId, existing.getDeptId());
        rule.setRuleId(ruleId);
        rule.setUpdateTime(LocalDateTime.now());
        validate(rule);
        if (workRuleMapper.update(rule) != 1) {
            throw new BusinessException("工作时间规则更新失败");
        }
        return rule;
    }

    @Override
    @Transactional
    public void disable(String ruleId, String doctorId) {
        if (workRuleMapper.disable(ruleId, doctorId) != 1) {
            throw new BusinessException("工作时间规则不存在");
        }
    }

    private Doctor requireEnabledDoctor(String doctorId) {
        Doctor doctor = doctorMapper.selectById(doctorId);
        if (doctor == null
                || !Integer.valueOf(1).equals(doctor.getStatus())
                || Integer.valueOf(1).equals(doctor.getIsDeleted())) {
            throw new BusinessException("医生不存在或已停用");
        }
        if (doctor.getDepartmentId() == null
                || doctor.getDepartmentId().isBlank()) {
            throw new BusinessException("医生尚未配置科室");
        }
        return doctor;
    }

    private DoctorWorkRule toEntity(
            DoctorWorkRuleRequest request,
            String doctorId,
            String deptId) {
        DoctorWorkRule rule = new DoctorWorkRule();
        rule.setDoctorId(doctorId);
        rule.setDeptId(deptId);
        rule.setDayOfWeek(request.getDayOfWeek());
        rule.setStartTime(request.getStartTime());
        rule.setEndTime(request.getEndTime());
        rule.setMaxPatients(request.getMaxPatients());
        rule.setPreferredLevel(request.getPreferredLevel());
        rule.setValidFrom(request.getValidFrom());
        rule.setValidTo(request.getValidTo());
        rule.setStatus(1);
        return rule;
    }

    private void validate(DoctorWorkRule rule) {
        if (!rule.getStartTime().isBefore(rule.getEndTime())) {
            throw new BusinessException("开始时间必须早于结束时间");
        }
        if (rule.getValidFrom().isAfter(rule.getValidTo())) {
            throw new BusinessException("生效日期不能晚于失效日期");
        }
        if (workRuleMapper.countOverlap(rule) > 0) {
            throw new BusinessException("工作时间规则与已有规则重叠");
        }
    }

    private String newId(String prefix) {
        return prefix + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 29);
    }
}

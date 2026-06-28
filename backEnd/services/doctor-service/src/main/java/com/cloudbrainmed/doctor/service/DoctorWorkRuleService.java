package com.cloudbrainmed.doctor.service;

import com.cloudbrainmed.doctor.dto.DoctorWorkRuleRequest;
import com.cloudbrainmed.doctor.entity.DoctorWorkRule;

import java.util.List;

public interface DoctorWorkRuleService {
    List<DoctorWorkRule> list(String doctorId);
    DoctorWorkRule create(
            DoctorWorkRuleRequest request, String doctorId);
    DoctorWorkRule update(
            String ruleId,
            DoctorWorkRuleRequest request,
            String doctorId);
    void disable(String ruleId, String doctorId);
}

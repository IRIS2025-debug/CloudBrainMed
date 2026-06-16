package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.doctor.entity.ConsultRecord;
import com.cloudbrainmed.doctor.entity.ExamOrder;
import com.cloudbrainmed.doctor.mapper.ConsultMapper;
import com.cloudbrainmed.doctor.mapper.ExamOrderMapper;
import com.cloudbrainmed.doctor.mapper.MedicalRecordMapper;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内部接口 — 供 ai-service 通过 Feign 反查病历上下文。
 * 路径 /internal/** 由 Gateway 屏蔽外部访问，仅服务间可调。
 */
@RestController
@RequestMapping("/internal/doctor/consult")
public class InternalConsultController {

    private final ConsultMapper consultMapper;
    private final MedicalRecordMapper medicalRecordMapper;
    private final ExamOrderMapper examOrderMapper;

    public InternalConsultController(ConsultMapper consultMapper,
                                     MedicalRecordMapper medicalRecordMapper,
                                     ExamOrderMapper examOrderMapper) {
        this.consultMapper = consultMapper;
        this.medicalRecordMapper = medicalRecordMapper;
        this.examOrderMapper = examOrderMapper;
    }

    /**
     * 获取接诊完整上下文（供 AI 接诊分析使用）
     *
     * @param registerId 挂号记录 ID
     * @return { chiefComplaint, recordDesc, patientAge, patientGender,
     *           historyRecords: [...], examReports: [...] }
     */
    @GetMapping("/context")
    public Map<String, Object> getConsultContext(@RequestParam("registerId") String registerId) {
        // 1. 查挂号记录
        ConsultRecord detail = consultMapper.findDetail(registerId);
        if (detail == null) {
            throw new BusinessException("就诊记录不存在");
        }

        String patientId = detail.getPatientId();

        // 2. 查历史病历（同一患者的历史就诊记录）
        List<Map<String, Object>> historyRecords =
                medicalRecordMapper.selectByPatientId(patientId);

        // 3. 查检查/检验报告
        List<ExamOrder> examReports =
                examOrderMapper.selectByPatientId(patientId);

        // 4. 组装返回
        Map<String, Object> context = new HashMap<>();
        context.put("registerId", registerId);
        context.put("patientId", patientId);
        context.put("chiefComplaint", detail.getChiefComplaint() != null
                ? detail.getChiefComplaint() : "");
        context.put("recordDesc", detail.getDescription() != null
                ? detail.getDescription() : "");
        context.put("patientAge", detail.getPatientAge() != null
                ? String.valueOf(detail.getPatientAge()) : "未知");
        context.put("patientGender", detail.getGender() != null
                ? (detail.getGender() == 1 ? "男" : "女") : "未知");
        context.put("patientName", detail.getPatientName() != null
                ? detail.getPatientName() : detail.getName());
        context.put("historyRecords", historyRecords != null ? historyRecords : List.of());
        context.put("examReports", examReports != null ? examReports : List.of());

        return context;
    }
}

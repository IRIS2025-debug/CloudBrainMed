package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.doctor.dto.SaveReportRequestDto;
import com.cloudbrainmed.doctor.entity.ExamOrder;
import com.cloudbrainmed.doctor.entity.MedicalReport;
import com.cloudbrainmed.doctor.mapper.ExamOrderMapper;
import com.cloudbrainmed.doctor.service.ExamOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ExamOrderServiceImpl implements ExamOrderService {

    private final ExamOrderMapper examOrderMapper;

    public ExamOrderServiceImpl(ExamOrderMapper examOrderMapper) {
        this.examOrderMapper = examOrderMapper;
    }

    @Override
    public List<ExamOrder> getByRegisterId(String registerId, String doctorId) {
        return examOrderMapper.selectByRegisterId(registerId, doctorId);
    }

    @Override
    public List<ExamOrder> getByDoctorId(String doctorId) {
        return examOrderMapper.selectByDoctorId(doctorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MedicalReport saveReport(SaveReportRequestDto request) {
        String orderItemId = request.getOrderItemId();

        // 1. 校验 orderItemId 是否存在，并获取 itemCategory
        String itemCategory = examOrderMapper.selectItemCategoryByOrderItemId(orderItemId);
        if (itemCategory == null || itemCategory.isEmpty()) {
            log.error("orderItemId: {} 在 medical_order_item 表中不存在", orderItemId);
            throw new IllegalArgumentException("检查项目不存在，请确认检查项目ID是否正确");
        }
        log.info("查询到 itemCategory: {}", itemCategory);

        // 2. 查询 assigned_doctor_id（当前检查医生ID）
        String assignedDoctorId = examOrderMapper.selectAssignedDoctorIdByOrderItemId(orderItemId);
        log.info("orderItemId: {} 对应的 assignedDoctorId: {}", orderItemId, assignedDoctorId);

        // 3. 构建并保存
        MedicalReport report = buildMedicalReport(request, itemCategory, assignedDoctorId);
        examOrderMapper.insertReport(report);

        // 4. 更新检查项目状态为 COMPLETED
        int updated = examOrderMapper.updateOrderItemStatusToCompleted(orderItemId);
        if (updated > 0) {
            log.info("检查项目状态更新成功，orderItemId: {}, status: COMPLETED", orderItemId);
        } else {
            log.warn("检查项目状态更新失败，orderItemId: {} 可能不存在", orderItemId);
        }
        return report;
    }

    private MedicalReport buildMedicalReport(SaveReportRequestDto request, String itemCategory, String assignedDoctorId) {
        MedicalReport report = new MedicalReport();

        report.setReportId(generateReportId());
        report.setOrderItemId(request.getOrderItemId());
        report.setPatientId(request.getRegisterId());
        report.setItemCategory(itemCategory);

        // 结果摘要
        String resultSummary = buildResultSummary(request.getComprehensive());
        report.setResultSummary(resultSummary);

        // 诊断结论
        String conclusion = request.getComprehensive().getDiagnosis();
        if (conclusion != null && conclusion.length() > 200) {
            conclusion = conclusion.substring(0, 200) + "...";
        }
        report.setConclusion(conclusion);

        // 异常标志
        report.setAbnormalFlag(determineAbnormalFlag(request));

        // 附件URL
        report.setAttachmentUrl(buildAttachmentUrl(request));

        // ===== 报告医师ID：使用 assignedDoctorId =====
        report.setReportDoctorId(assignedDoctorId);

        // 随访建议
        report.setFollowUpAdvice(request.getComprehensive().getAdvice());

        // 状态：已发布
        report.setStatus("PUBLISHED");

        // 时间
        LocalDateTime now = LocalDateTime.now();
        report.setPerformedTime(now);
        report.setReportTime(now);
        report.setCreateTime(now);
        report.setUpdateTime(now);

        return report;
    }

    private String generateReportId() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return String.format("RPT-%s-%s", date, uuid);
    }

    private String buildResultSummary(SaveReportRequestDto.ComprehensiveInfoDto comprehensive) {
        StringBuilder sb = new StringBuilder();

        if (comprehensive.getFindings() != null && !comprehensive.getFindings().isEmpty()) {
            sb.append("【影像所见】\n").append(comprehensive.getFindings());
        }

        if (comprehensive.getDiagnosis() != null && !comprehensive.getDiagnosis().isEmpty()) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append("【诊断意见】\n").append(comprehensive.getDiagnosis());
        }

        return sb.toString();
    }

    private String determineAbnormalFlag(SaveReportRequestDto request) {
        if (request.getLesion() != null && request.getLesion().getRiskLevel() != null) {
            String riskLevel = request.getLesion().getRiskLevel();
            if ("高".equals(riskLevel) || "极高".equals(riskLevel)) {
                return "ABNORMAL";
            } else if ("中".equals(riskLevel)) {
                return "SUSPICIOUS";
            }
        }

        if (request.getLesion() != null && request.getLesion().getFindings() != null) {
            String findings = request.getLesion().getFindings();
            if (findings.contains("检出") || findings.contains("发现")) {
                return "SUSPICIOUS";
            }
        }

        return "NORMAL";
    }

    private String buildAttachmentUrl(SaveReportRequestDto request) {
        StringBuilder sb = new StringBuilder("[");
        boolean hasImage = false;

        if (request.getImages() != null) {
            if (request.getImages().getArtifact() != null && !request.getImages().getArtifact().isEmpty()) {
                if (hasImage) sb.append(",");
                sb.append("\"").append(request.getImages().getArtifact()).append("\"");
                hasImage = true;
            }
            if (request.getImages().getLesion() != null && !request.getImages().getLesion().isEmpty()) {
                if (hasImage) sb.append(",");
                sb.append("\"").append(request.getImages().getLesion()).append("\"");
                hasImage = true;
            }
        }

        sb.append("]");
        return hasImage ? sb.toString() : null;
    }
}
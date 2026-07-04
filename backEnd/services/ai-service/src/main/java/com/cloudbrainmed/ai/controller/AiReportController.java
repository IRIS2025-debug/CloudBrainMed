package com.cloudbrainmed.ai.controller;

import com.cloudbrainmed.ai.dto.ReportAnalysisDto;
import com.cloudbrainmed.ai.service.AiReportService;
import com.cloudbrainmed.ai.support.DoctorAuthHelper;
import com.cloudbrainmed.ai.vo.ReportAnalysisVo;
import com.cloudbrainmed.api.dto.ReportContextDto;
import com.cloudbrainmed.api.feign.DoctorFeignClient;
import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.result.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai-service/report")
public class AiReportController {

    private final AiReportService aiReportService;
    private final DoctorFeignClient doctorFeignClient;
    private final String internalServiceKey;

    public AiReportController(AiReportService aiReportService,
                              DoctorFeignClient doctorFeignClient,
                              @Value("${internal.service-key:}") String internalServiceKey) {
        this.aiReportService = aiReportService;
        this.doctorFeignClient = doctorFeignClient;
        this.internalServiceKey = internalServiceKey;
    }

    @PostMapping("/analyze")
    public Result<ReportAnalysisVo> analyze(
            @RequestHeader(value = "token", required = false) String token,
            @RequestBody ReportAnalysisDto dto) {
        String doctorId = DoctorAuthHelper.requireDoctorId(token, "AI报告分析");
        if (dto == null || !StringUtils.hasText(dto.getRegisterId())) {
            throw new BusinessException("挂号ID不能为空");
        }
        if (!isCtStructuredReport(dto)) {
            ReportContextDto context = doctorFeignClient.getConsultContext(
                    dto.getRegisterId(), doctorId, internalServiceKey);
            if (context == null || !context.isAvailable()) {
                throw new BusinessException(context == null
                        ? "无法获取患者接诊信息"
                        : context.getErrorMessage());
            }
        }
        return Result.ok(aiReportService.analyze(dto));
    }

    private boolean isCtStructuredReport(ReportAnalysisDto dto) {
        if (dto.getReportInput() == null || dto.getReportInput().isEmpty()) {
            return false;
        }
        String reportType = dto.getReportType();
        return "CT_ARTIFACT_REPORT".equals(reportType)
                || "CT_LESION_REPORT".equals(reportType);
    }
}

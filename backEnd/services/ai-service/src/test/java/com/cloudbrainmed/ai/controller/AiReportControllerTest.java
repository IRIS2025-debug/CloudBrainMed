package com.cloudbrainmed.ai.controller;

import com.cloudbrainmed.ai.dto.ReportAnalysisDto;
import com.cloudbrainmed.ai.service.AiReportService;
import com.cloudbrainmed.ai.vo.ReportAnalysisVo;
import com.cloudbrainmed.api.dto.ReportContextDto;
import com.cloudbrainmed.api.feign.DoctorFeignClient;
import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class AiReportControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void analyzeRejectsRequestWithoutDoctorToken() throws Exception {
        AiReportService reportService = mock(AiReportService.class);
        DoctorFeignClient doctorFeignClient = mock(DoctorFeignClient.class);
        when(reportService.analyze(any())).thenReturn(new ReportAnalysisVo());
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new AiReportController(
                        reportService, doctorFeignClient, "internal-key"))
                .build();

        ReportAnalysisDto request = new ReportAnalysisDto();
        request.setRegisterId("REG001");
        request.setReportType("LAB");
        request.setReportText("WBC 12.5");

        assertThatThrownBy(() -> mockMvc.perform(post("/ai-service/report/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))))
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("未登录，请先登录");

        verify(reportService, never()).analyze(any());
    }

    @Test
    void analyzeLoadsConsultContextBeforeReportAnalysis() {
        AiReportService reportService = mock(AiReportService.class);
        DoctorFeignClient doctorFeignClient = mock(DoctorFeignClient.class);
        ReportContextDto context = new ReportContextDto();
        context.setAvailable(true);
        when(doctorFeignClient.getConsultContext("REG001", "D001", "internal-key"))
                .thenReturn(context);
        ReportAnalysisDto request = new ReportAnalysisDto();
        request.setRegisterId("REG001");
        request.setReportType("LAB");
        request.setReportText("WBC 12.5");

        new AiReportController(reportService, doctorFeignClient, "internal-key")
                .analyze(doctorToken(), request);

        verify(doctorFeignClient).getConsultContext("REG001", "D001", "internal-key");
        verify(reportService).analyze(request);
    }

    @Test
    void analyzeCtStructuredReportAllowsExamDoctorWithoutConsultContextLookup() {
        AiReportService reportService = mock(AiReportService.class);
        DoctorFeignClient doctorFeignClient = mock(DoctorFeignClient.class);
        ReportAnalysisDto request = new ReportAnalysisDto();
        request.setRegisterId("REG001");
        request.setReportType("CT_LESION_REPORT");
        request.setReportInput(Map.of(
                "task", "CT_LESION_REPORT",
                "finding", Map.of("lesionDetected", true)));

        new AiReportController(reportService, doctorFeignClient, "internal-key")
                .analyze(examDoctorToken(), request);

        verify(doctorFeignClient, never()).getConsultContext(any(), any(), any());
        verify(reportService).analyze(request);
    }

    @Test
    void analyzeRejectsWhenConsultContextUnavailable() {
        AiReportService reportService = mock(AiReportService.class);
        DoctorFeignClient doctorFeignClient = mock(DoctorFeignClient.class);
        ReportContextDto context = new ReportContextDto();
        context.setAvailable(false);
        context.setErrorMessage("接诊已完成，不能再次发起AI分析");
        when(doctorFeignClient.getConsultContext("REG001", "D001", "internal-key"))
                .thenReturn(context);
        ReportAnalysisDto request = new ReportAnalysisDto();
        request.setRegisterId("REG001");
        request.setReportType("LAB");
        request.setReportText("WBC 12.5");

        assertThatThrownBy(() -> new AiReportController(
                reportService, doctorFeignClient, "internal-key")
                .analyze(doctorToken(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("接诊已完成，不能再次发起AI分析");

        verify(reportService, never()).analyze(any());
    }

    @Test
    void analyzeRejectsBlankRegisterIdBeforeLoadingConsultContext() {
        AiReportService reportService = mock(AiReportService.class);
        DoctorFeignClient doctorFeignClient = mock(DoctorFeignClient.class);
        ReportAnalysisDto request = new ReportAnalysisDto();
        request.setRegisterId(" ");
        request.setReportType("LAB");
        request.setReportText("WBC 12.5");

        assertThatThrownBy(() -> new AiReportController(
                reportService, doctorFeignClient, "internal-key")
                .analyze(doctorToken(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("挂号ID不能为空");

        verify(doctorFeignClient, never()).getConsultContext(any(), any(), any());
        verify(reportService, never()).analyze(any());
    }

    private String doctorToken() {
        return DoctorJwtUtil.createToken("D001", "13800000000", 2);
    }

    private String examDoctorToken() {
        return DoctorJwtUtil.createToken("D002", "13800000001", 2, 2);
    }
}

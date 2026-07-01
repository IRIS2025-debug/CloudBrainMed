package com.cloudbrainmed.ai.service.impl;

import com.cloudbrainmed.ai.dto.ReportAnalysisDto;
import com.cloudbrainmed.ai.entity.AiInferenceLog;
import com.cloudbrainmed.ai.mapper.AiInferenceLogMapper;
import com.cloudbrainmed.ai.vo.ReportAnalysisVo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiReportServiceImplTest {

    private ChatClient chatClient;
    private AiInferenceLogMapper inferenceLogMapper;
    private AiReportServiceImpl service;

    @BeforeEach
    void setUp() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(builder.build()).thenReturn(chatClient);
        inferenceLogMapper = mock(AiInferenceLogMapper.class);
        service = new AiReportServiceImpl(builder, new ObjectMapper(), inferenceLogMapper);
    }

    @Test
    void analyzePersistsInferenceLogWhenModelFailsAndFallbackIsReturned() {
        when(chatClient.prompt(any(Prompt.class)).call().content()).thenThrow(new RuntimeException("model down"));

        ReportAnalysisVo response = service.analyze(request());

        assertThat(response.getFallback()).isTrue();
        verify(inferenceLogMapper).insert(argThat((AiInferenceLog log) ->
                "REPORT_ANALYSIS".equals(log.getCallSource())
                        && "REG001".equals(log.getTraceId())
                        && "FALLBACK".equals(log.getStatus())
                        && log.getInputSummary().contains("LAB")
                        && log.getOutputSummary().contains("MEDIUM")));
    }

    private ReportAnalysisDto request() {
        ReportAnalysisDto dto = new ReportAnalysisDto();
        dto.setRegisterId("REG001");
        dto.setReportType("LAB");
        dto.setReportText("WBC 12.5");
        return dto;
    }
}

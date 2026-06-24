package com.cloudbrainmed.ai.service.impl;

import com.cloudbrainmed.ai.dto.AiRecordGenerateRequest;
import com.cloudbrainmed.ai.dto.AiRecordGenerateResponse;
import com.cloudbrainmed.ai.entity.AiInferenceLog;
import com.cloudbrainmed.ai.mapper.AiInferenceLogMapper;
import com.cloudbrainmed.api.dto.ReportContextDto;
import com.cloudbrainmed.api.feign.DoctorFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiMedicalRecordServiceImplTest {

    private DoctorFeignClient doctorClient;
    private AiInferenceLogMapper logMapper;
    private ChatClient chatClient;
    private AiMedicalRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(builder.build()).thenReturn(chatClient);
        doctorClient = mock(DoctorFeignClient.class);
        logMapper = mock(AiInferenceLogMapper.class);
        service = new AiMedicalRecordServiceImpl(
                builder,
                new ObjectMapper(),
                doctorClient,
                logMapper,
                "test-model",
                "internal-key");
    }

    @Test
    void generateCreatesStructuredEditableDraft() {
        when(doctorClient.getConsultContext(
                "REG001", "D001", "internal-key"))
                .thenReturn(context());
        when(chatClient.prompt(any(Prompt.class)).call().content())
                .thenReturn("""
                    {
                      "informationCompleteness": "SUFFICIENT",
                      "structuredRecord": {
                        "chiefComplaint": "反复头痛伴眩晕3天",
                        "historyOfPresentIllness": "三天前出现头痛伴眩晕",
                        "pastMedicalHistory": "待补充",
                        "allergyHistory": "待补充",
                        "personalHistory": "待补充",
                        "familyHistory": "待补充",
                        "physicalExamination": "待补充",
                        "auxiliaryExamination": "待补充",
                        "assessment": "头痛待查",
                        "treatmentPlan": "完善相关检查"
                      },
                      "draftRecordDesc": "主诉：反复头痛伴眩晕3天。现病史：三天前出现头痛伴眩晕。",
                      "missingInformation": ["既往史"],
                      "riskLevel": "MEDIUM",
                      "riskWarnings": ["需排除神经系统急症"]
                    }
                    """);

        AiRecordGenerateResponse response = service.generate(
                request(), "D001");

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.isFallback()).isFalse();
        assertThat(response.getStructuredRecord().getChiefComplaint())
                .isEqualTo("反复头痛伴眩晕3天");
        assertThat(response.getDraftRecordDesc()).contains("主诉");

        ArgumentCaptor<AiInferenceLog> captor =
                ArgumentCaptor.forClass(AiInferenceLog.class);
        verify(logMapper).insert(captor.capture());
        assertThat(captor.getValue().getCallSource())
                .isEqualTo("AI_MEDICAL_RECORD_GENERATE");
    }

    @Test
    void generateFallsBackWhenModelReturnsInvalidContent() {
        ReportContextDto context = context();
        context.setCurrentRecordDesc("医生已有草稿");
        when(doctorClient.getConsultContext(
                "REG001", "D001", "internal-key"))
                .thenReturn(context);
        when(chatClient.prompt(any(Prompt.class)).call().content())
                .thenReturn("invalid");

        AiRecordGenerateResponse response = service.generate(
                request(), "D001");

        assertThat(response.isFallback()).isTrue();
        assertThat(response.getDraftRecordDesc()).isEqualTo("医生已有草稿");
    }

    @Test
    void generatePropagatesUnavailableContext() {
        ReportContextDto context = new ReportContextDto();
        context.setAvailable(false);
        context.setErrorMessage("无权访问该患者的接诊信息");
        when(doctorClient.getConsultContext(
                "REG001", "D002", "internal-key"))
                .thenReturn(context);

        assertThatThrownBy(() -> service.generate(request(), "D002"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("无权访问");
    }

    private AiRecordGenerateRequest request() {
        AiRecordGenerateRequest request = new AiRecordGenerateRequest();
        request.setRegisterId("REG001");
        request.setConversationText("医生：哪里不舒服？患者：头痛并伴有眩晕三天。");
        request.setStructuredParameters(Map.of("疼痛程度", "中度"));
        return request;
    }

    private ReportContextDto context() {
        ReportContextDto context = new ReportContextDto();
        context.setRegisterId("REG001");
        context.setPatientId("P001");
        context.setPatientAge(35);
        context.setPatientGender("男");
        context.setChiefComplaint("头痛伴眩晕");
        return context;
    }
}

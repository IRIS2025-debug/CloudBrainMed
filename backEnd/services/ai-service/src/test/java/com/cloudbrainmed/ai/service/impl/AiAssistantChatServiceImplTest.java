package com.cloudbrainmed.ai.service.impl;

import com.cloudbrainmed.ai.dto.AiAssistantChatRequest;
import com.cloudbrainmed.ai.dto.AiAssistantChatResponse;
import com.cloudbrainmed.ai.dto.AiRecordGenerateRequest;
import com.cloudbrainmed.ai.dto.AiRecordGenerateResponse;
import com.cloudbrainmed.ai.dto.PrescriptionReviewMedicineRequest;
import com.cloudbrainmed.ai.dto.PrescriptionReviewRequest;
import com.cloudbrainmed.ai.dto.PrescriptionReviewResponse;
import com.cloudbrainmed.ai.entity.AiInferenceLog;
import com.cloudbrainmed.ai.mapper.AiInferenceLogMapper;
import com.cloudbrainmed.ai.service.AiMedicalRecordService;
import com.cloudbrainmed.ai.service.AiPrescriptionReviewService;
import com.cloudbrainmed.api.dto.ReportContextDto;
import com.cloudbrainmed.api.feign.DoctorFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiAssistantChatServiceImplTest {

    private DoctorFeignClient doctorClient;
    private AiInferenceLogMapper logMapper;
    private AiMedicalRecordService medicalRecordService;
    private AiPrescriptionReviewService prescriptionReviewService;
    private ChatClient chatClient;
    private AiAssistantChatServiceImpl service;

    @BeforeEach
    void setUp() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(builder.build()).thenReturn(chatClient);
        doctorClient = mock(DoctorFeignClient.class);
        logMapper = mock(AiInferenceLogMapper.class);
        medicalRecordService = mock(AiMedicalRecordService.class);
        prescriptionReviewService = mock(AiPrescriptionReviewService.class);
        service = new AiAssistantChatServiceImpl(
                builder,
                new ObjectMapper(),
                doctorClient,
                logMapper,
                medicalRecordService,
                prescriptionReviewService,
                "test-model",
                "internal-key");
    }

    @Test
    void chatAnswersFollowUpQuestionsInsideAssistantBoundary() {
        prepareContext();
        when(chatClient.prompt(any(Prompt.class)).call().content())
                .thenReturn("建议继续追问发病时间、诱因、持续时间和伴随症状。");

        AiAssistantChatResponse response = service.chat(
                request("这个患者还需要问哪些问题？"), "D001");

        assertThat(response.isHandledByAssistant()).isTrue();
        assertThat(response.getIntent()).isEqualTo("FOLLOW_UP_QUESTION");
        assertThat(response.getAnswer()).contains("发病时间");

        ArgumentCaptor<AiInferenceLog> captor =
                ArgumentCaptor.forClass(AiInferenceLog.class);
        verify(logMapper).insert(captor.capture());
        assertThat(captor.getValue().getCallSource())
                .isEqualTo("AI_ASSISTANT_CHAT");
        assertThat(captor.getValue().getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void chatAnswersDiagnosisAssistantInsideAssistantBoundary() {
        prepareContext();
        when(chatClient.prompt(any(Prompt.class)).call().content())
                .thenReturn("可能诊断：偏头痛。依据：头痛2天；仍需补充神经系统查体和伴随症状。");

        AiAssistantChatResponse response = service.chat(
                request("根据目前信息可能是什么病？"), "D001");

        assertThat(response.isHandledByAssistant()).isTrue();
        assertThat(response.getIntent()).isEqualTo("DIAGNOSIS_ASSISTANT");
        assertThat(response.getAnswer()).contains("可能诊断");
        assertThat(response.getModuleResult()).isNull();
        verify(medicalRecordService, never()).generate(any(), any());
        verify(prescriptionReviewService, never()).review(any(), any());
    }

    @Test
    void chatDelegatesMedicalRecordDraftWithoutCallingChatModel() {
        prepareContext();
        AiRecordGenerateResponse generated = new AiRecordGenerateResponse();
        generated.setStatus("SUCCESS");
        generated.setDraftRecordDesc("主诉：头痛2天。");
        when(medicalRecordService.generate(
                any(AiRecordGenerateRequest.class), any()))
                .thenReturn(generated);

        AiAssistantChatResponse response = service.chat(
                request("帮我生成病历草稿"), "D001");

        assertThat(response.isHandledByAssistant()).isFalse();
        assertThat(response.getIntent()).isEqualTo("MEDICAL_RECORD_DRAFT");
        assertThat(response.getHandledModule()).isEqualTo("AI_MEDICAL_RECORD");
        assertThat(response.getModuleResult()).isSameAs(generated);
        verify(medicalRecordService).generate(
                any(AiRecordGenerateRequest.class), any());
        verify(chatClient, never()).prompt(any(Prompt.class));
    }

    @Test
    void chatDelegatesPrescriptionReviewWhenMedicinesAreProvided() {
        prepareContext();
        PrescriptionReviewResponse review = new PrescriptionReviewResponse();
        review.setStatus("SUCCESS");
        review.setSummary("未发现明显相互作用");
        when(prescriptionReviewService.review(
                any(PrescriptionReviewRequest.class), any()))
                .thenReturn(review);
        AiAssistantChatRequest request = request(
                "这个患者青霉素过敏，用药需要注意什么？");
        PrescriptionReviewMedicineRequest medicine =
                new PrescriptionReviewMedicineRequest();
        medicine.setMedicineId("MED001");
        medicine.setUsage("口服，每日一次");
        medicine.setQuantity(1);
        request.setMedicines(List.of(medicine));

        AiAssistantChatResponse response = service.chat(request, "D001");

        assertThat(response.isHandledByAssistant()).isFalse();
        assertThat(response.getIntent()).isEqualTo("PRESCRIPTION_REVIEW");
        assertThat(response.getHandledModule())
                .isEqualTo("AI_PRESCRIPTION_REVIEW");
        assertThat(response.getModuleResult()).isSameAs(review);
        verify(prescriptionReviewService).review(
                any(PrescriptionReviewRequest.class), any());
        verify(chatClient, never()).prompt(any(Prompt.class));
    }

    private AiAssistantChatRequest request(String message) {
        AiAssistantChatRequest request = new AiAssistantChatRequest();
        request.setRegisterId("REG001");
        request.setMessage(message);
        return request;
    }

    private void prepareContext() {
        ReportContextDto context = new ReportContextDto();
        context.setRegisterId("REG001");
        context.setPatientId("P001");
        context.setPatientAge(42);
        context.setPatientGender("男");
        context.setChiefComplaint("头痛2天");
        when(doctorClient.getConsultContext(
                "REG001", "D001", "internal-key"))
                .thenReturn(context);
    }
}

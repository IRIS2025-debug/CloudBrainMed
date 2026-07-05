package com.cloudbrainmed.ai.service.impl;

import com.cloudbrainmed.ai.dto.AiAssistantChatRequest;
import com.cloudbrainmed.ai.dto.AiAssistantChatResponse;
import com.cloudbrainmed.ai.dto.AiRecordGenerateRequest;
import com.cloudbrainmed.ai.dto.AiRecordGenerateResponse;
import com.cloudbrainmed.ai.dto.PrescriptionDraftResponse;
import com.cloudbrainmed.ai.dto.PrescriptionReviewMedicineRequest;
import com.cloudbrainmed.ai.dto.PrescriptionReviewRequest;
import com.cloudbrainmed.ai.dto.PrescriptionReviewResponse;
import com.cloudbrainmed.ai.mapper.MedicineMapper;
import com.cloudbrainmed.ai.service.AiMedicalRecordService;
import com.cloudbrainmed.ai.service.AiPrescriptionDraftService;
import com.cloudbrainmed.ai.service.AiPrescriptionReviewService;
import com.cloudbrainmed.api.dto.ReportContextDto;
import com.cloudbrainmed.api.feign.DoctorFeignClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vectorstore.VectorStore;

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
    private AiMedicalRecordService medicalRecordService;
    private AiPrescriptionDraftService prescriptionDraftService;
    private AiPrescriptionReviewService prescriptionReviewService;
    private MedicineMapper medicineMapper;
    private VectorStore vectorStore;
    private ChatClient chatClient;
    private AiAssistantChatServiceImpl service;

    @BeforeEach
    void setUp() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(builder.build()).thenReturn(chatClient);
        doctorClient = mock(DoctorFeignClient.class);
        medicalRecordService = mock(AiMedicalRecordService.class);
        prescriptionDraftService = mock(AiPrescriptionDraftService.class);
        prescriptionReviewService = mock(AiPrescriptionReviewService.class);
        medicineMapper = mock(MedicineMapper.class);
        vectorStore = mock(VectorStore.class);
        service = new AiAssistantChatServiceImpl(
                builder,
                doctorClient,
                medicalRecordService,
                prescriptionDraftService,
                prescriptionReviewService,
                medicineMapper,
                vectorStore,
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
        assertThat(response.getIntent()).isEqualTo("RECEPTION_ASSISTANT");
        assertThat(response.getAnswer()).contains("发病时间");}

    @Test
    void chatAnswersDiagnosisQuestionsInsideReceptionAssistantBoundary() {
        prepareContext();
        when(chatClient.prompt(any(Prompt.class)).call().content())
                .thenReturn("可能诊断：偏头痛。依据：头痛2天；仍需补充神经系统查体和伴随症状。");

        AiAssistantChatResponse response = service.chat(
                request("根据目前信息可能是什么病？"), "D001");

        assertThat(response.isHandledByAssistant()).isTrue();
        assertThat(response.getIntent()).isEqualTo("RECEPTION_ASSISTANT");
        assertThat(response.getAnswer()).contains("可能诊断");
        assertThat(response.getModuleResult()).isNull();
        verify(medicalRecordService, never()).generate(any(), any());
        verify(prescriptionDraftService, never()).generate(any(), any());
        verify(prescriptionReviewService, never()).review(any(), any());
    }

    @Test
    void chatNormalizesMarkdownFormattingFromModelAnswer() {
        prepareContext();
        when(chatClient.prompt(any(Prompt.class)).call().content())
                .thenReturn("""
                        ### Known
                        - Patient female
                        1. **Pain detail**
                        - Fever?
                        """);

        AiAssistantChatRequest request = request(null);
        request.setActionType("MISSING_INFORMATION");

        AiAssistantChatResponse response = service.chat(request, "D001");

        assertThat(response.getAnswer()).contains("Known");
        assertThat(response.getAnswer()).contains("Patient female");
        assertThat(response.getAnswer()).contains("Pain detail");
        assertThat(response.getAnswer()).contains("Fever?");
        assertThat(response.getAnswer()).doesNotContain("###");
        assertThat(response.getAnswer()).doesNotContain("**");
        assertThat(response.getAnswer()).doesNotContain("- Patient");
        assertThat(response.getAnswer()).doesNotContain("1. Pain");
    }

    @Test
    void receptionAssistantFallsBackWhenConsultContextUnavailable() {
        AiAssistantChatRequest request = request(null);
        request.setActionType("RECEPTION_ASSISTANT");
        when(doctorClient.getConsultContext(
                "REG001", "D001", "internal-key"))
                .thenThrow(new IllegalStateException("context unavailable"));

        AiAssistantChatResponse response = service.chat(request, "D001");

        assertThat(response.getIntent()).isEqualTo("RECEPTION_ASSISTANT");
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.isFallback()).isTrue();
        assertThat(response.isHandledByAssistant()).isTrue();
        verify(chatClient, never()).prompt(any(Prompt.class));
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

        AiAssistantChatRequest request = request("帮我生成病历草稿");
        request.setActionType("MEDICAL_RECORD_DRAFT");

        AiAssistantChatResponse response = service.chat(request, "D001");

        assertThat(response.isHandledByAssistant()).isFalse();
        assertThat(response.getIntent()).isEqualTo("MEDICAL_RECORD_DRAFT");
        assertThat(response.getHandledModule()).isEqualTo("AI_MEDICAL_RECORD");
        assertThat(response.getStatus()).isEqualTo("DELEGATED");
        assertThat(response.getModuleResult()).isSameAs(generated);
        verify(medicalRecordService).generate(
                any(AiRecordGenerateRequest.class), any());
        verify(chatClient, never()).prompt(any(Prompt.class));
    }

    @Test
    void actionTypeCanDelegateWithoutFreeTextMessage() {
        prepareContext();
        AiRecordGenerateResponse generated = new AiRecordGenerateResponse();
        generated.setStatus("SUCCESS");
        generated.setDraftRecordDesc("主诉：头痛2天。");
        when(medicalRecordService.generate(
                any(AiRecordGenerateRequest.class), any()))
                .thenReturn(generated);
        AiAssistantChatRequest request = request(null);
        request.setActionType("MEDICAL_RECORD_DRAFT");
        request.setConversationText("患者诉头痛2天");

        AiAssistantChatResponse response = service.chat(request, "D001");

        assertThat(response.getIntent()).isEqualTo("MEDICAL_RECORD_DRAFT");
        assertThat(response.getStatus()).isEqualTo("DELEGATED");
        verify(medicalRecordService).generate(
                any(AiRecordGenerateRequest.class), any());
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
        request.setActionType("PRESCRIPTION_REVIEW");
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
        assertThat(response.getStatus()).isEqualTo("DELEGATED");
        assertThat(response.getModuleResult()).isSameAs(review);
        verify(prescriptionReviewService).review(
                any(PrescriptionReviewRequest.class), any());
        verify(chatClient, never()).prompt(any(Prompt.class));
    }

    @Test
    void chatDelegatesPrescriptionDraftWithoutCallingReceptionModel() {
        PrescriptionDraftResponse draft = new PrescriptionDraftResponse();
        draft.setStatus("SUCCESS");
        draft.setSummary("建议处方草稿，需医生确认。");
        when(prescriptionDraftService.generate(any(), any()))
                .thenReturn(draft);
        AiAssistantChatRequest request = request("根据当前病情生成处方草稿");
        request.setActionType("PRESCRIPTION_DRAFT");

        AiAssistantChatResponse response = service.chat(request, "D001");

        assertThat(response.isHandledByAssistant()).isFalse();
        assertThat(response.getIntent()).isEqualTo("PRESCRIPTION_DRAFT");
        assertThat(response.getHandledModule())
                .isEqualTo("AI_PRESCRIPTION_DRAFT");
        assertThat(response.getStatus()).isEqualTo("DELEGATED");
        assertThat(response.getModuleResult()).isSameAs(draft);
        verify(prescriptionDraftService).generate(any(), any());
        verify(chatClient, never()).prompt(any(Prompt.class));
    }

    @Test
    void prescriptionReviewNeedsMedicineListBeforeDelegation() {
        prepareContext();

        AiAssistantChatRequest request = request("帮我审核这张处方有没有用药风险");
        request.setActionType("PRESCRIPTION_REVIEW");

        AiAssistantChatResponse response = service.chat(request, "D001");

        assertThat(response.isHandledByAssistant()).isFalse();
        assertThat(response.getIntent()).isEqualTo("PRESCRIPTION_REVIEW");
        assertThat(response.getStatus()).isEqualTo("NEEDS_INPUT");
        assertThat(response.getModuleResult()).isNull();
        assertThat(response.getAnswer()).contains("待审核药品列表");
        verify(prescriptionReviewService, never()).review(any(), any());}

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

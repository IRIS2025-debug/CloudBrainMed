package com.cloudbrainmed.ai.service.impl;

import com.cloudbrainmed.ai.dto.AiAssistantChatRequest;
import com.cloudbrainmed.ai.dto.PrescriptionDraftResponse;
import com.cloudbrainmed.ai.dto.PrescriptionReviewResponse;
import com.cloudbrainmed.ai.entity.Medicine;
import com.cloudbrainmed.ai.mapper.MedicineMapper;
import com.cloudbrainmed.ai.service.AiPrescriptionReviewService;
import com.cloudbrainmed.api.dto.ReportContextDto;
import com.cloudbrainmed.api.feign.DoctorFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiPrescriptionDraftServiceImplTest {

    private ChatClient chatClient;
    private DoctorFeignClient doctorClient;
    private MedicineMapper medicineMapper;
    private AiPrescriptionReviewService reviewService;
    private AiPrescriptionDraftServiceImpl service;

    @BeforeEach
    void setUp() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(builder.build()).thenReturn(chatClient);
        doctorClient = mock(DoctorFeignClient.class);
        medicineMapper = mock(MedicineMapper.class);
        reviewService = mock(AiPrescriptionReviewService.class);
        service = new AiPrescriptionDraftServiceImpl(
                builder,
                new ObjectMapper(),
                doctorClient,
                medicineMapper,
                reviewService,
                "test-model",
                "internal-key");
    }

    @Test
    void generateDiscardsMedicineOutsideDatabaseCandidates() {
        prepareContext();
        when(medicineMapper.selectAll()).thenReturn(List.of(
                medicine("MED001", "数据库药品", "10mg", 20)));
        when(chatClient.prompt(any(Prompt.class)).call().content())
                .thenReturn("""
                    {
                      "summary": "建议对症处理",
                      "overallRiskLevel": "LOW",
                      "medicines": [
                        {
                          "medicineId": "MED001",
                          "medicineName": "模型错误名称",
                          "spec": "模型错误规格",
                          "usage": "口服，每日一次",
                          "quantity": 2,
                          "reason": "用于对症处理",
                          "warnings": []
                        },
                        {
                          "medicineId": "UNKNOWN",
                          "medicineName": "库外药品",
                          "usage": "口服",
                          "quantity": 1
                        }
                      ],
                      "missingInformation": [],
                      "warnings": []
                    }
                    """);
        when(reviewService.review(any(), eq("D001")))
                .thenReturn(new PrescriptionReviewResponse());

        PrescriptionDraftResponse response = service.generate(
                request(), "D001");

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getMedicines()).hasSize(1);
        assertThat(response.getMedicines().get(0).getMedicineId())
                .isEqualTo("MED001");
        assertThat(response.getMedicines().get(0).getMedicineName())
                .isEqualTo("数据库药品");
        assertThat(response.getMedicines().get(0).getSpec())
                .isEqualTo("10mg");
        assertThat(response.getWarnings())
                .anyMatch(value -> value.contains("药品数据库候选列表"));
        verify(reviewService).review(any(), eq("D001"));
    }

    @Test
    void generateStopsBeforeModelWhenMedicineDatabaseIsEmpty() {
        prepareContext();
        when(medicineMapper.selectAll()).thenReturn(List.of());

        PrescriptionDraftResponse response = service.generate(
                request(), "D001");

        assertThat(response.getStatus()).isEqualTo("NEEDS_INPUT");
        assertThat(response.getMedicines()).isEmpty();
        assertThat(response.getSummary()).contains("药品数据库");
        verify(chatClient, never()).prompt(any(Prompt.class));
        verify(reviewService, never()).review(any(), any());
    }

    private void prepareContext() {
        ReportContextDto context = new ReportContextDto();
        context.setAvailable(true);
        context.setRegisterId("REG001");
        context.setPatientId("P001");
        context.setPatientAge(35);
        context.setPatientGender("男");
        context.setChiefComplaint("咳嗽三天");
        when(doctorClient.getConsultContext(
                "REG001", "D001", "internal-key"))
                .thenReturn(context);
    }

    private AiAssistantChatRequest request() {
        AiAssistantChatRequest request = new AiAssistantChatRequest();
        request.setRegisterId("REG001");
        request.setMessage("请根据当前接诊信息生成处方草稿");
        request.setCurrentRecordDesc("主诉：咳嗽三天");
        request.setStructuredParameters(Map.of("诊断意见", "上呼吸道感染待确认"));
        return request;
    }

    private Medicine medicine(
            String id, String name, String spec, int stock) {
        Medicine medicine = new Medicine();
        medicine.setMedicineId(id);
        medicine.setName(name);
        medicine.setSpec(spec);
        medicine.setUsage("遵医嘱");
        medicine.setIndication("示例适应症");
        medicine.setAttention("使用前核对禁忌和过敏史");
        medicine.setStock(stock);
        return medicine;
    }
}

package com.cloudbrainmed.ai.service.impl;

import com.cloudbrainmed.ai.dto.PrescriptionReviewMedicineRequest;
import com.cloudbrainmed.ai.dto.PrescriptionReviewRequest;
import com.cloudbrainmed.ai.dto.PrescriptionReviewResponse;
import com.cloudbrainmed.ai.entity.Medicine;
import com.cloudbrainmed.ai.mapper.MedicineMapper;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiPrescriptionReviewServiceImplTest {

    private DoctorFeignClient doctorClient;
    private MedicineMapper medicineMapper;
    private ChatClient chatClient;
    private AiPrescriptionReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(builder.build()).thenReturn(chatClient);
        doctorClient = mock(DoctorFeignClient.class);
        medicineMapper = mock(MedicineMapper.class);
        service = new AiPrescriptionReviewServiceImpl(
                builder,
                new ObjectMapper(),
                doctorClient,
                medicineMapper,
                "test-model",
                "internal-key");
    }

    @Test
    void reviewReturnsInteractionAndDatabaseMedicineSnapshot() {
        prepareContext();
        when(medicineMapper.selectById("MED001"))
                .thenReturn(medicine("MED001", "药品A", 20));
        when(medicineMapper.selectById("MED002"))
                .thenReturn(medicine("MED002", "药品B", 20));
        when(chatClient.prompt(any(Prompt.class)).call().content())
                .thenReturn("""
                    {
                      "passed": false,
                      "overallRiskLevel": "HIGH",
                      "summary": "存在需要医生确认的药物相互作用",
                      "interactions": [{
                        "medicineA": "药品A",
                        "medicineB": "药品B",
                        "severity": "HIGH",
                        "description": "可能增加不良反应风险",
                        "recommendation": "评估替代方案"
                      }],
                      "medicineRisks": [{
                        "medicineId": "MED001",
                        "medicineName": "错误名称",
                        "riskLevel": "MEDIUM",
                        "issues": ["需关注用药风险"],
                        "suggestions": ["加强监测"]
                      }],
                      "contraindications": [],
                      "recommendations": ["由医生确认后提交"],
                      "missingInformation": []
                    }
                    """);

        PrescriptionReviewResponse response = service.review(
                request(List.of(
                        item("MED001", 1), item("MED002", 1))),
                "D001");

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.isPassed()).isFalse();
        assertThat(response.getInteractions()).hasSize(1);
        assertThat(response.getMedicineRisks().get(0).getMedicineName())
                .isEqualTo("药品A");}

    @Test
    void reviewForcesHighRiskWhenStockIsInsufficient() {
        prepareContext();
        when(medicineMapper.selectById("MED001"))
                .thenReturn(medicine("MED001", "药品A", 0));
        when(chatClient.prompt(any(Prompt.class)).call().content())
                .thenReturn("""
                    {
                      "passed": true,
                      "overallRiskLevel": "LOW",
                      "summary": "未发现明显风险",
                      "interactions": [],
                      "medicineRisks": [],
                      "contraindications": [],
                      "recommendations": [],
                      "missingInformation": []
                    }
                    """);

        PrescriptionReviewResponse response = service.review(
                request(List.of(item("MED001", 2))), "D001");

        assertThat(response.isPassed()).isFalse();
        assertThat(response.getOverallRiskLevel()).isEqualTo("HIGH");
        assertThat(response.getMedicineRisks().get(0).getIssues())
                .anyMatch(value -> value.contains("库存不足"));
    }

    @Test
    void reviewRejectsUnknownMedicineBeforeCallingModel() {
        prepareContext();
        when(medicineMapper.selectById("UNKNOWN")).thenReturn(null);

        assertThatThrownBy(() -> service.review(
                request(List.of(item("UNKNOWN", 1))), "D001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("药品不存在");
    }

    private void prepareContext() {
        ReportContextDto context = new ReportContextDto();
        context.setRegisterId("REG001");
        context.setPatientId("P001");
        context.setPatientAge(60);
        context.setPatientGender("女");
        context.setChiefComplaint("头痛");
        context.setCurrentRecordDesc("高血压病史");
        context.setMedicalHistory(List.of("既往高血压"));
        when(doctorClient.getConsultContext(
                "REG001", "D001", "internal-key"))
                .thenReturn(context);
    }

    private PrescriptionReviewRequest request(
            List<PrescriptionReviewMedicineRequest> medicines) {
        PrescriptionReviewRequest request = new PrescriptionReviewRequest();
        request.setRegisterId("REG001");
        request.setCurrentRecordDesc("高血压病史");
        request.setPatientInformation(Map.of("过敏史", "待补充"));
        request.setMedicines(medicines);
        return request;
    }

    private PrescriptionReviewMedicineRequest item(
            String medicineId, int quantity) {
        PrescriptionReviewMedicineRequest item =
                new PrescriptionReviewMedicineRequest();
        item.setMedicineId(medicineId);
        item.setUsage("口服，每日一次");
        item.setQuantity(quantity);
        return item;
    }

    private Medicine medicine(String id, String name, int stock) {
        Medicine medicine = new Medicine();
        medicine.setMedicineId(id);
        medicine.setName(name);
        medicine.setSpec("10mg");
        medicine.setUsage("遵医嘱");
        medicine.setIndication("示例适应症");
        medicine.setAttention("使用前核对禁忌和过敏史");
        medicine.setStock(stock);
        return medicine;
    }
}

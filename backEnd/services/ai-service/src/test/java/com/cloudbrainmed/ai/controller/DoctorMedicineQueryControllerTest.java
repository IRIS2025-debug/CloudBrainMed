package com.cloudbrainmed.ai.controller;

import com.cloudbrainmed.ai.dto.DoctorMedicineChatRequest;
import com.cloudbrainmed.ai.service.DoctorMedicineQueryService;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DoctorMedicineQueryControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private DoctorMedicineQueryService queryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        queryService = mock(DoctorMedicineQueryService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DoctorMedicineQueryController(queryService))
                .build();
    }

    @Test
    void chatRejectsRequestWithoutDoctorToken() throws Exception {
        DoctorMedicineChatRequest request = new DoctorMedicineChatRequest();
        request.setQuestion("布洛芬适应症");

        assertThatThrownBy(() -> mockMvc.perform(
                post("/ai-service/medicine/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))))
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("未登录，请先登录");

        verify(queryService, never()).query(any());
    }

    @Test
    void chatRejectsNonDoctorToken() throws Exception {
        DoctorMedicineChatRequest request = new DoctorMedicineChatRequest();
        request.setQuestion("布洛芬适应症");

        // roleType=1 非医生
        String patientToken = DoctorJwtUtil.createToken(
                "U001", "13800000000", 1);

        assertThatThrownBy(() -> mockMvc.perform(
                post("/ai-service/medicine/chat")
                        .header("token", patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))))
                .hasRootCauseInstanceOf(IllegalArgumentException.class);

        verify(queryService, never()).query(any());
    }

    @Test
    void chatReturnsBadRequestWhenQuestionBlank() throws Exception {
        DoctorMedicineChatRequest request = new DoctorMedicineChatRequest();
        request.setQuestion("   ");

        mockMvc.perform(post("/ai-service/medicine/chat")
                        .header("token", doctorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(queryService, never()).query(any());
    }

    @Test
    void chatReturnsPlainTextAnswerForDoctor() throws Exception {
        when(queryService.query(anyString()))
                .thenReturn("布洛芬适用于疼痛与发热，消化道溃疡患者慎用。");

        DoctorMedicineChatRequest request = new DoctorMedicineChatRequest();
        request.setQuestion("布洛芬适应症");

        String body = mockMvc.perform(post("/ai-service/medicine/chat")
                        .header("token", doctorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        assertThat(body)
                .isEqualTo("布洛芬适用于疼痛与发热，消化道溃疡患者慎用。");
        verify(queryService).query("布洛芬适应症");
    }

    private String doctorToken() {
        return DoctorJwtUtil.createToken("D001", "13800000000", 2);
    }
}

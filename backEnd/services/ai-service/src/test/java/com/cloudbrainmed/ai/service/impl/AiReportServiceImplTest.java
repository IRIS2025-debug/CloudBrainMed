package com.cloudbrainmed.ai.service.impl;

import com.cloudbrainmed.ai.dto.AiFeedbackRequest;
import com.cloudbrainmed.ai.mapper.AiFeedbackSampleMapper;
import com.cloudbrainmed.ai.mapper.AiInferenceLogMapper;
import com.cloudbrainmed.api.dto.ReportContextDto;
import com.cloudbrainmed.api.feign.DoctorFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiReportServiceImplTest {

    private final AiInferenceLogMapper logMapper =
            mock(AiInferenceLogMapper.class);
    private final AiFeedbackSampleMapper feedbackMapper =
            mock(AiFeedbackSampleMapper.class);
    private final DoctorFeignClient doctorClient =
            mock(DoctorFeignClient.class);
    private AiReportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiReportServiceImpl(
                new ObjectMapper(),
                logMapper,
                feedbackMapper,
                doctorClient,
                "internal-key");
    }

    @Test
    void feedbackIsIdempotentForSameTraceAndDoctor() {
        when(logMapper.findInputByTraceId("AI001"))
                .thenReturn("{\"registerId\":\"R001\"}");
        ReportContextDto context = new ReportContextDto();
        when(doctorClient.getConsultContext(
                "R001", "D001", "internal-key"))
                .thenReturn(context);
        when(feedbackMapper.countByTraceIdAndDoctorId("AI001", "D001"))
                .thenReturn(1);

        assertTrue(service.saveFeedback(feedback(), "D001"));
        verify(feedbackMapper, never()).insert(
                any(), any(), any(), any(), anyShort(),
                any(), any(), any());
    }

    @Test
    void feedbackRejectsTraceWithoutSuccessfulInference() {
        when(logMapper.findInputByTraceId("AI001")).thenReturn(null);

        assertFalse(service.saveFeedback(feedback(), "D001"));
        verify(doctorClient, never()).getConsultContext(
                any(), any(), any());
    }

    private AiFeedbackRequest feedback() {
        AiFeedbackRequest request = new AiFeedbackRequest();
        request.setTraceId("AI001");
        request.setFinalRecordDesc("医生确认病历");
        request.setAdoptionType("FULL");
        return request;
    }
}

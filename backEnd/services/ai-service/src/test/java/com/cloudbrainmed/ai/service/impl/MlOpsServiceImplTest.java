package com.cloudbrainmed.ai.service.impl;

import com.cloudbrainmed.ai.mapper.AiInferenceLogMapper;
import com.cloudbrainmed.ai.model.InferenceEngine;
import com.cloudbrainmed.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MlOpsServiceImplTest {

    private AiInferenceLogMapper inferenceLogMapper;
    private InferenceEngine inferenceEngine;
    private MlOpsServiceImpl service;

    @BeforeEach
    void setUp() {
        inferenceLogMapper = mock(AiInferenceLogMapper.class);
        inferenceEngine = mock(InferenceEngine.class);
        service = new MlOpsServiceImpl(inferenceLogMapper, inferenceEngine);
    }

    @Test
    void getModelStatsDoesNotDependOnModelRegistryAfterManagementRemoval() {
        when(inferenceLogMapper.countAll()).thenReturn(10);

        var result = service.getModelStats();

        assertThat(result)
                .containsEntry("activeModels", 0)
                .containsEntry("totalInference", 10);
    }

    @Test
    void getInferenceStatsComputesSuccessRateFromInferenceLogStatus() {
        when(inferenceLogMapper.countToday()).thenReturn(2);
        when(inferenceLogMapper.countAll()).thenReturn(4);
        when(inferenceLogMapper.countByStatus("SUCCESS")).thenReturn(3);
        when(inferenceLogMapper.avgLatency()).thenReturn(123.4);

        var result = service.getInferenceStats();

        assertThat(result).containsEntry("todayTotal", 2)
                .containsEntry("successRate", 75.0)
                .containsEntry("avgLatency", 123L);
    }

    @Test
    void getInferenceLogsRejectsInvalidPaginationBeforeQueryingMapper() {
        assertThatThrownBy(() -> service.getInferenceLogs(0, 10))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分页");

        verify(inferenceLogMapper, never()).selectPage(
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void checkPythonServiceReportsHealthWithoutActiveModelLookup() {
        when(inferenceEngine.isPythonServiceAlive()).thenReturn(true);

        Map<String, Object> result = service.checkPythonService();

        assertThat(result)
                .containsEntry("pythonServiceAlive", true)
                .containsEntry("activeModel", "disabled");
    }
}

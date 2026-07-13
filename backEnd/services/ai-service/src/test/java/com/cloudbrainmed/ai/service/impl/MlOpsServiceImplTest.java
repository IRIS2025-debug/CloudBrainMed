package com.cloudbrainmed.ai.service.impl;

import com.cloudbrainmed.ai.mapper.AiInferenceLogMapper;
import com.cloudbrainmed.ai.mapper.ModelVersionMapper;
import com.cloudbrainmed.ai.model.InferenceEngine;
import com.cloudbrainmed.ai.model.ModelLoader;
import com.cloudbrainmed.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MlOpsServiceImplTest {

    private ModelVersionMapper modelVersionMapper;
    private AiInferenceLogMapper inferenceLogMapper;
    private ModelLoader modelLoader;
    private InferenceEngine inferenceEngine;
    private MlOpsServiceImpl service;

    @BeforeEach
    void setUp() {
        modelVersionMapper = mock(ModelVersionMapper.class);
        inferenceLogMapper = mock(AiInferenceLogMapper.class);
        modelLoader = mock(ModelLoader.class);
        inferenceEngine = mock(InferenceEngine.class);
        service = new MlOpsServiceImpl(
                inferenceLogMapper,
                modelVersionMapper,
                modelLoader,
                inferenceEngine);
    }

    @Test
    void getModelStatsCountsActiveModelsFromDatabase() {
        when(modelVersionMapper.countByStatus("ACTIVE")).thenReturn(2);
        when(inferenceLogMapper.countAll()).thenReturn(10);

        var result = service.getModelStats();

        assertThat(result).containsEntry("activeModels", 2)
                .containsEntry("totalInference", 10);
    }

    @Test
    void getInferenceStatsComputesSuccessRateWithoutAdoptionRate() {
        when(inferenceLogMapper.countToday()).thenReturn(2);
        when(inferenceLogMapper.countAll()).thenReturn(4);
        when(inferenceLogMapper.countByStatus("SUCCESS")).thenReturn(3);
        when(inferenceLogMapper.avgLatency()).thenReturn(123.4);

        var result = service.getInferenceStats();

        assertThat(result).containsEntry("todayTotal", 2)
                .containsEntry("successRate", 75.0)
                .containsEntry("avgLatency", 123L)
                // 采纳率没有稳定业务写入来源，已从看板移除。
                .doesNotContainKey("adoptionRate");
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
    void getModelListReturnsExactlyTwoBusinessModelsWhenPythonHealthy() {
        when(inferenceEngine.getPythonHealth()).thenReturn(Map.of(
                "model_type", "unet",
                "model_version", "2.0",
                "lesion_model_type", "attention_unet",
                "lesion_model_version", "1.3",
                "lesion_fallback", false));

        List<Map<String, Object>> models = service.getModelList();

        assertThat(models).hasSize(2);
        assertThat(models.get(0)).containsEntry("modelKey", "ct-artifact-model")
                .containsEntry("displayName", "CT 金属伪影识别")
                .containsEntry("version", "2.0")
                .containsEntry("status", "READY");
        assertThat(models.get(1)).containsEntry("modelKey", "ct-lesion-model")
                .containsEntry("displayName", "CT 病灶识别/分割")
                .containsEntry("version", "1.3")
                .containsEntry("status", "READY");
    }

    @Test
    void getModelListMarksLesionModelFallbackWhenPythonReportsNoWeights() {
        when(inferenceEngine.getPythonHealth()).thenReturn(Map.of(
                "model_version", "2.0",
                "lesion_model_version", "heuristic_no_weights",
                "lesion_fallback", true));

        List<Map<String, Object>> models = service.getModelList();

        assertThat(models).hasSize(2);
        assertThat(models.get(0)).containsEntry("status", "READY");
        // 病灶模型无权重时降级，前端显示「降级」。
        assertThat(models.get(1)).containsEntry("status", "FALLBACK")
                .containsEntry("version", "heuristic_no_weights");
    }

    @Test
    void getModelListShowsBothModelsOfflineWhenPythonUnreachable() {
        when(inferenceEngine.getPythonHealth()).thenReturn(null);

        List<Map<String, Object>> models = service.getModelList();

        // Python 不可达时仍展示两个模型，版本 "--"、状态离线，绝不回落到 ai_model_registry 历史记录。
        assertThat(models).hasSize(2);
        assertThat(models.get(0)).containsEntry("modelKey", "ct-artifact-model")
                .containsEntry("version", "--")
                .containsEntry("status", "OFFLINE");
        assertThat(models.get(1)).containsEntry("modelKey", "ct-lesion-model")
                .containsEntry("version", "--")
                .containsEntry("status", "OFFLINE");
        verify(modelVersionMapper, never()).selectAll();
    }
}

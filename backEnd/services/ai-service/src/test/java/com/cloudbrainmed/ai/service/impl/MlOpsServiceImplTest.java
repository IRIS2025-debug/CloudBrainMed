package com.cloudbrainmed.ai.service.impl;

import com.cloudbrainmed.ai.entity.ModelVersion;
import com.cloudbrainmed.ai.mapper.AiInferenceLogMapper;
import com.cloudbrainmed.ai.mapper.ModelVersionMapper;
import com.cloudbrainmed.ai.mapper.TrainingSampleMapper;
import com.cloudbrainmed.ai.model.CnnModel;
import com.cloudbrainmed.ai.model.InferenceEngine;
import com.cloudbrainmed.ai.model.ModelLoader;
import com.cloudbrainmed.ai.model.ModelTrainer;
import com.cloudbrainmed.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
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
    private TrainingSampleMapper sampleMapper;
    private ModelLoader modelLoader;
    private ModelTrainer modelTrainer;
    private MlOpsServiceImpl service;

    @BeforeEach
    void setUp() {
        modelVersionMapper = mock(ModelVersionMapper.class);
        inferenceLogMapper = mock(AiInferenceLogMapper.class);
        sampleMapper = mock(TrainingSampleMapper.class);
        modelLoader = mock(ModelLoader.class);
        modelTrainer = mock(ModelTrainer.class);
        service = new MlOpsServiceImpl(
                inferenceLogMapper,
                modelVersionMapper,
                sampleMapper,
                modelLoader,
                modelTrainer,
                mock(InferenceEngine.class));
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
    void getInferenceStatsComputesSuccessRateFromInferenceLogStatus() {
        when(inferenceLogMapper.countToday()).thenReturn(2);
        when(inferenceLogMapper.countAll()).thenReturn(4);
        when(inferenceLogMapper.countByStatus("SUCCESS")).thenReturn(3);
        when(inferenceLogMapper.avgLatency()).thenReturn(123.4);
        when(sampleMapper.countAll()).thenReturn(5);
        when(sampleMapper.countAdopted()).thenReturn(3);

        var result = service.getInferenceStats();

        assertThat(result).containsEntry("todayTotal", 2)
                .containsEntry("successRate", 75.0)
                .containsEntry("avgLatency", 123L)
                .containsEntry("adoptionRate", 60.0);
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
    void getSampleListRejectsInvalidPaginationBeforeQueryingMapper() {
        assertThatThrownBy(() -> service.getSampleList(1, 0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分页");

        verify(sampleMapper, never()).selectPage(
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void updateSampleRejectsBlankSampleIdBeforeUpdatingLabel() {
        assertThatThrownBy(() -> service.updateSample(" ", "报告", "MANUAL"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("样本ID");

        verify(sampleMapper, never()).updateLabel(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateSampleRejectsBlankLabelBeforeMarkingSampleLabeled() {
        assertThatThrownBy(() -> service.updateSample("S001", " ", "MANUAL"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("标签");

        verify(sampleMapper, never()).updateLabel(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateSampleRejectsUnknownSampleIdWhenNoRowIsUpdated() {
        when(sampleMapper.updateLabel("S404", "报告正常", "MANUAL", "LABELED"))
                .thenReturn(0);

        assertThatThrownBy(() -> service.updateSample("S404", "报告正常", "MANUAL"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("样本不存在");
    }

    @Test
    void setModelTrafficActivatesModelWhenTrafficIsFull() {
        ModelVersion model = new ModelVersion();
        model.setModelId("MOD001");
        when(modelVersionMapper.selectById("MOD001")).thenReturn(model);

        var result = service.setModelTraffic("MOD001", 100);

        assertThat(result).containsEntry("modelId", "MOD001")
                .containsEntry("trafficPct", 100)
                .containsEntry("status", "ACTIVE");
        verify(modelLoader).activateModel("MOD001");
    }

    @Test
    void setModelTrafficMarksModelInactiveWhenTrafficIsZero() {
        ModelVersion model = new ModelVersion();
        model.setModelId("MOD001");
        when(modelVersionMapper.selectById("MOD001")).thenReturn(model);

        var result = service.setModelTraffic("MOD001", 0);

        assertThat(result).containsEntry("status", "INACTIVE");
        verify(modelVersionMapper).updateStatus("MOD001", "INACTIVE");
    }

    @Test
    void setModelTrafficRejectsPartialTrafficUntilWeightedRoutingExists() {
        assertThatThrownBy(() -> service.setModelTraffic("MOD001", 50))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅支持0或100");
    }

    @Test
    void triggerTrainRejectsUnknownModelTypeBeforeCreatingTask() {
        assertThatThrownBy(() -> service.triggerTrain(Map.of("modelType", "resnet")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("模型类型");

        verify(modelTrainer, never()).createTrainingTask(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void triggerTrainUsesDefaultsWhenOptionalTextParamsAreBlank() {
        ModelTrainer.TrainingTask task = new ModelTrainer.TrainingTask();
        task.setTaskId("TRN001");
        task.setStatus("PENDING");
        when(modelTrainer.createTrainingTask(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(task);

        service.triggerTrain(Map.of(
                "modelKey", " ",
                "modelType", "unet",
                "datasetPath", " "));

        ArgumentCaptor<String> modelKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> datasetPathCaptor = ArgumentCaptor.forClass(String.class);
        verify(modelTrainer).createTrainingTask(
                modelKeyCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(CnnModel.ModelType.UNET),
                org.mockito.ArgumentMatchers.any(),
                datasetPathCaptor.capture());
        assertThat(modelKeyCaptor.getValue()).isEqualTo("medical-ct-unet");
        assertThat(datasetPathCaptor.getValue()).isEqualTo("/data/ct-artifact/");
    }

    @Test
    void triggerTrainPassesTunedHyperParamsToTrainingTaskForAuditTrail() {
        ModelTrainer.TrainingTask task = new ModelTrainer.TrainingTask();
        task.setTaskId("TRN001");
        task.setStatus("PENDING");
        when(modelTrainer.createTrainingTask(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(task);

        service.triggerTrain(Map.of(
                "learningRate", "0.0005",
                "epochs", "80",
                "batchSize", "8",
                "optimizer", "Adam"));

        ArgumentCaptor<CnnModel.HyperParams> paramsCaptor =
                ArgumentCaptor.forClass(CnnModel.HyperParams.class);
        verify(modelTrainer).createTrainingTask(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                paramsCaptor.capture(),
                org.mockito.ArgumentMatchers.any());
        CnnModel.HyperParams params = paramsCaptor.getValue();
        assertThat(params.getLearningRate()).isEqualTo(0.0005);
        assertThat(params.getEpochs()).isEqualTo(80);
        assertThat(params.getBatchSize()).isEqualTo(8);
        assertThat(params.getOptimizer()).isEqualTo("Adam");
    }

    @Test
    void triggerTrainRejectsInvalidNumericHyperParamBeforeCreatingTask() {
        assertThatThrownBy(() -> service.triggerTrain(Map.of("learningRate", "fast")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("超参数");

        verify(modelTrainer, never()).createTrainingTask(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void triggerTrainRejectsOutOfRangeHyperParamsBeforeCreatingTask() {
        assertThatThrownBy(() -> service.triggerTrain(Map.of("learningRate", "0")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("超参数");
        assertThatThrownBy(() -> service.triggerTrain(Map.of("epochs", "-1")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("超参数");
        assertThatThrownBy(() -> service.triggerTrain(Map.of("batchSize", "0")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("超参数");

        verify(modelTrainer, never()).createTrainingTask(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void triggerTrainRejectsNonFiniteLearningRateBeforeCreatingTask() {
        assertThatThrownBy(() -> service.triggerTrain(Map.of("learningRate", "NaN")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("超参数");
        assertThatThrownBy(() -> service.triggerTrain(Map.of("learningRate", "Infinity")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("超参数");

        verify(modelTrainer, never()).createTrainingTask(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getTrainingTasksReturnsDetailFieldsForAdminTaskTable() {
        ModelTrainer.TrainingTask task = new ModelTrainer.TrainingTask();
        CnnModel.HyperParams params = CnnModel.HyperParams.defaultParams();
        params.setLearningRate(0.0005);
        params.setEpochs(80);
        params.setBatchSize(8);
        params.setOptimizer("Adam");
        task.setTaskId("TRN001");
        task.setModelKey("medical-ct-unet");
        task.setModelType("unet");
        task.setHyperParams(params);
        task.setDatasetPath("/data/ct-six-groups/exp-01");
        task.setStatus("FAILED");
        task.setModelId("MOD001");
        task.setErrorMessage("dataset not found");
        task.setCreateTime(LocalDateTime.of(2026, 6, 29, 20, 0));
        task.setStartTime(LocalDateTime.of(2026, 6, 29, 20, 1));
        task.setCompletedTime(LocalDateTime.of(2026, 6, 29, 20, 2));
        when(modelTrainer.listTasks()).thenReturn(Map.of("TRN001", task));
        when(modelTrainer.getStats()).thenReturn(Map.of("failed", 1L));

        var result = service.getTrainingTasks();
        var tasks = (java.util.List<Map<String, Object>>) result.get("tasks");

        assertThat(tasks).singleElement()
                .satisfies(item -> assertThat(item)
                        .containsEntry("taskId", "TRN001")
                        .containsEntry("modelId", "MOD001")
                        .containsEntry("datasetPath", "/data/ct-six-groups/exp-01")
                        .containsEntry("errorMessage", "dataset not found")
                        .containsEntry("startTime", LocalDateTime.of(2026, 6, 29, 20, 1))
                        .containsEntry("completedTime", LocalDateTime.of(2026, 6, 29, 20, 2))
                        .extracting(row -> (Map<String, Object>) row.get("hyperParams"))
                        .satisfies(hyperParams -> assertThat(hyperParams)
                                .containsEntry("learningRate", 0.0005)
                                .containsEntry("epochs", 80)
                                .containsEntry("batchSize", 8)
                                .containsEntry("optimizer", "Adam")));
    }
}

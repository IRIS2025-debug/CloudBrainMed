package com.cloudbrainmed.ai.model;

import com.cloudbrainmed.ai.entity.TrainingTask;
import com.cloudbrainmed.ai.mapper.ModelVersionMapper;
import com.cloudbrainmed.ai.mapper.TrainingTaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModelTrainerTest {

    private TrainingTaskMapper trainingTaskMapper;
    private ModelTrainer modelTrainer;

    @BeforeEach
    void setUp() {
        trainingTaskMapper = mock(TrainingTaskMapper.class);
        modelTrainer = new ModelTrainer(
                mock(ModelVersionMapper.class),
                mock(ModelLoader.class),
                trainingTaskMapper);
    }

    @Test
    void createTrainingTaskPersistsTaskForAuditTrail() {
        CnnModel.HyperParams params = CnnModel.HyperParams.defaultParams();

        ModelTrainer.TrainingTask task = modelTrainer.createTrainingTask(
                "medical-ct-unet",
                CnnModel.ModelType.UNET,
                params,
                "/data/ct-artifact");

        assertThat(task.getTaskId()).startsWith("TRN");
        verify(trainingTaskMapper).insert(any(TrainingTask.class));
    }

    @Test
    void listTasksReadsPersistedTaskHistory() {
        TrainingTask task = new TrainingTask();
        task.setTaskId("TRN001");
        task.setModelKey("medical-ct-unet");
        task.setModelType("unet");
        task.setStatus("COMPLETED");
        when(trainingTaskMapper.selectAll()).thenReturn(List.of(task));

        var result = modelTrainer.listTasks();

        assertThat(result).containsKey("TRN001");
        assertThat(result.get("TRN001").getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void listTasksUsesDefaultHyperParamsWhenPersistedHistoryHasNoHyperParams() {
        TrainingTask task = new TrainingTask();
        task.setTaskId("TRN001");
        task.setModelKey("medical-ct-unet");
        task.setModelType("unet");
        task.setStatus("FAILED");
        when(trainingTaskMapper.selectAll()).thenReturn(List.of(task));

        var result = modelTrainer.listTasks();

        assertThat(result.get("TRN001").getHyperParams()).isNotNull();
    }

    @Test
    void listTasksUsesDefaultHyperParamsWhenPersistedHistoryHasInvalidHyperParams() {
        TrainingTask task = new TrainingTask();
        task.setTaskId("TRN001");
        task.setModelKey("medical-ct-unet");
        task.setModelType("unet");
        task.setStatus("FAILED");
        task.setHyperParams("not-json");
        when(trainingTaskMapper.selectAll()).thenReturn(List.of(task));

        var result = modelTrainer.listTasks();

        assertThat(result.get("TRN001").getHyperParams()).isNotNull();
    }

    @Test
    void listTasksPreservesMapperOrderForAdminTaskTable() {
        TrainingTask newest = new TrainingTask();
        newest.setTaskId("TRN_NEW");
        newest.setModelKey("medical-ct-unet");
        newest.setModelType("unet");
        newest.setStatus("RUNNING");

        TrainingTask older = new TrainingTask();
        older.setTaskId("TRN_OLD");
        older.setModelKey("medical-ct-unet");
        older.setModelType("unet");
        older.setStatus("COMPLETED");
        when(trainingTaskMapper.selectAll()).thenReturn(List.of(newest, older));

        var result = modelTrainer.listTasks();

        assertThat(new ArrayList<>(result.keySet())).containsExactly("TRN_NEW", "TRN_OLD");
    }
}

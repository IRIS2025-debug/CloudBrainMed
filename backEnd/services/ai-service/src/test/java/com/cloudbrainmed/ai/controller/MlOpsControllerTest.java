package com.cloudbrainmed.ai.controller;

import com.cloudbrainmed.ai.service.MlOpsService;
import com.cloudbrainmed.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MlOpsControllerTest {

    private final MlOpsService service = mock(MlOpsService.class);
    private final MlOpsController controller = new MlOpsController(service);

    @Test
    void sampleListAliasReturnsListForFrontendContract() {
        when(service.getSampleList(1, 20)).thenReturn(Map.of("list", java.util.List.of(), "total", 0));

        Object data = controller.sampleListAlias(1, 20).getData();

        assertThat(data).isEqualTo(Map.of("list", java.util.List.of(), "total", 0));
        verify(service).getSampleList(1, 20);
    }

    @Test
    void labelSampleAliasMapsFrontendFieldsToServiceFields() {
        controller.labelSample(Map.of("sampleId", "S001", "labelTag", "报告"));

        verify(service).updateSample("S001", "报告", "MANUAL");
    }

    @Test
    void modelTrainAliasAcceptsEmptyBody() {
        when(service.triggerTrain(Map.of())).thenReturn(Map.of("taskId", "T001", "status", "RUNNING"));

        Object data = controller.triggerTrainAlias(null).getData();

        assertThat(data).isEqualTo(Map.of("taskId", "T001", "status", "RUNNING"));
        verify(service).triggerTrain(Map.of());
    }

    @Test
    void modelTrafficAliasDelegatesTrafficUpdateToService() {
        when(service.setModelTraffic("MOD001", 100))
                .thenReturn(Map.of("modelId", "MOD001", "trafficPct", 100, "status", "ACTIVE"));

        Object data = controller.setModelTraffic(Map.of("modelId", "MOD001", "trafficPct", 100)).getData();

        assertThat(data).isEqualTo(Map.of("modelId", "MOD001", "trafficPct", 100, "status", "ACTIVE"));
        verify(service).setModelTraffic("MOD001", 100);
    }

    @Test
    void modelTrafficAliasRejectsInvalidTrafficPctBeforeServiceCall() {
        assertThatThrownBy(() -> controller.setModelTraffic(Map.of("modelId", "MOD001", "trafficPct", "abc")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("流量配置");
    }

    @Test
    void ctInferenceDelegatesUploadToMlService() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "ct.nii.gz",
                "application/octet-stream", new byte[] {1});
        when(service.predictCtArtifact(file)).thenReturn(Map.of("status", "success"));

        Object data = controller.predictCtArtifact(file).getData();

        assertThat(data).isEqualTo(Map.of("status", "success"));
        verify(service).predictCtArtifact(file);
    }

    @Test
    void ctInferenceResultDownloadDelegatesMaskFileToMlService() throws Exception {
        byte[] maskBytes = "mask".getBytes(StandardCharsets.UTF_8);
        when(service.downloadCtArtifactMask("scan_mask.nii.gz"))
                .thenReturn(ResponseEntity.ok(maskBytes));

        ResponseEntity<byte[]> response = controller.downloadCtArtifactMask("scan_mask.nii.gz");

        assertThat(response.getBody()).isEqualTo(maskBytes);
        verify(service).downloadCtArtifactMask("scan_mask.nii.gz");
    }
}

package com.cloudbrainmed.ai.controller;

import com.cloudbrainmed.ai.service.MlOpsService;
import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MlOpsControllerTest {

    private final MlOpsService service = mock(MlOpsService.class);
    private final MlOpsController controller =
            new MlOpsController(service, "internal-key");
    private final Map<String, String> adminHeaders = Map.of(
            "token", DoctorJwtUtil.createToken("A001", "13700000001", 3));
    private final Map<String, String> doctorHeaders = Map.of(
            "token", DoctorJwtUtil.createToken("D001", "13900000001", 2, 2));
    private final Map<String, String> internalHeaders = Map.of(
            "X-Internal-Service-Key", "internal-key");

    @Test
    void modelManagementAndTrainingEndpointsAreNotExposed() {
        String mappedPaths = Arrays.stream(MlOpsController.class.getDeclaredMethods())
                .flatMap(method -> Arrays.stream(method.getAnnotations()))
                .map(Object::toString)
                .collect(Collectors.joining("\n"));

        assertThat(mappedPaths)
                .doesNotContain("/models/list")
                .doesNotContain("/model/list")
                .doesNotContain("/models/train")
                .doesNotContain("/model/train")
                .doesNotContain("/model/traffic")
                .doesNotContain("/models/tasks");
    }
    @Test
    void ctInferenceDelegatesUploadToMlService() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "ct.nii.gz",
                "application/octet-stream", new byte[] {1});
        when(service.predictCtArtifact(file)).thenReturn(Map.of("status", "success"));

        Object data = controller.predictCtArtifact(adminHeaders, file).getData();

        assertThat(data).isEqualTo(Map.of("status", "success"));
        verify(service).predictCtArtifact(file);
    }

    @Test
    void ctInferenceResultDownloadDelegatesMaskFileToMlService() throws Exception {
        byte[] maskBytes = "mask".getBytes(StandardCharsets.UTF_8);
        when(service.downloadCtArtifactMask("scan_mask.nii.gz"))
                .thenReturn(ResponseEntity.ok(maskBytes));

        ResponseEntity<byte[]> response = controller.downloadCtArtifactMask(adminHeaders, "scan_mask.nii.gz");

        assertThat(response.getBody()).isEqualTo(maskBytes);
        verify(service).downloadCtArtifactMask("scan_mask.nii.gz");
    }

    @Test
    void ctInferencePreviewDownloadDelegatesPreviewFileToMlService() throws Exception {
        byte[] previewBytes = "png".getBytes(StandardCharsets.UTF_8);
        when(service.downloadCtArtifactPreview("scan_preview_z1.png"))
                .thenReturn(ResponseEntity.ok(previewBytes));

        ResponseEntity<byte[]> response = controller.downloadCtArtifactPreview(adminHeaders, "scan_preview_z1.png");

        assertThat(response.getBody()).isEqualTo(previewBytes);
        verify(service).downloadCtArtifactPreview("scan_preview_z1.png");
    }

    @Test
    void ctLesionInferenceDelegatesUploadToMlService() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "ct.nii.gz",
                "application/octet-stream", new byte[] {1});
        when(service.predictCtLesion(file)).thenReturn(Map.of("status", "success"));

        Object data = controller.predictCtLesion(adminHeaders, file).getData();

        assertThat(data).isEqualTo(Map.of("status", "success"));
        verify(service).predictCtLesion(file);
    }

    @Test
    void ctLesionResultDownloadDelegatesMaskFileToMlService() throws Exception {
        byte[] maskBytes = "mask".getBytes(StandardCharsets.UTF_8);
        when(service.downloadCtLesionMask("scan_lesion_mask.nii.gz"))
                .thenReturn(ResponseEntity.ok(maskBytes));

        ResponseEntity<byte[]> response = controller.downloadCtLesionMask(adminHeaders, "scan_lesion_mask.nii.gz");

        assertThat(response.getBody()).isEqualTo(maskBytes);
        verify(service).downloadCtLesionMask("scan_lesion_mask.nii.gz");
    }

    @Test
    void ctLesionPreviewDownloadDelegatesPreviewFileToMlService() throws Exception {
        byte[] previewBytes = "png".getBytes(StandardCharsets.UTF_8);
        when(service.downloadCtLesionPreview("scan_lesion_preview_z1.png"))
                .thenReturn(ResponseEntity.ok(previewBytes));

        ResponseEntity<byte[]> response = controller.downloadCtLesionPreview(adminHeaders, "scan_lesion_preview_z1.png");

        assertThat(response.getBody()).isEqualTo(previewBytes);
        verify(service).downloadCtLesionPreview("scan_lesion_preview_z1.png");
    }

    @Test
    void inferenceStatsRejectsNonAdminToken() {
        assertThatThrownBy(() -> controller.inferenceStats(doctorHeaders))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅管理员");
    }
    @Test
    void ctInferenceAllowsTrustedInternalRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "ct.nii.gz",
                "application/octet-stream", new byte[] {1});
        when(service.predictCtArtifact(file)).thenReturn(Map.of("status", "success"));

        Object data = controller.predictCtArtifact(internalHeaders, file).getData();

        assertThat(data).isEqualTo(Map.of("status", "success"));
        verify(service).predictCtArtifact(file);
    }
}

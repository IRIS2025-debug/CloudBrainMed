package com.cloudbrainmed.ai.controller;

import com.cloudbrainmed.ai.service.MlOpsService;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MlOpsControllerTest {

    private final MlOpsService service = mock(MlOpsService.class);
    private final MlOpsController controller = new MlOpsController(service);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

    @Test
    void dashboardRejectsMissingAdminToken() throws Exception {
        mockMvc.perform(get("/admin-service/ml/dashboard/inference-stats"))
                .andExpect(jsonPath("$.code").value(401));

        verifyNoInteractions(service);
    }

    @Test
    void dashboardRejectsInvalidAdminToken() throws Exception {
        mockMvc.perform(get("/admin-service/ml/dashboard/inference-stats")
                        .header("token", "not-a-jwt"))
                .andExpect(jsonPath("$.code").value(401));

        verifyNoInteractions(service);
    }

    @Test
    void dashboardRejectsDoctorToken() throws Exception {
        mockMvc.perform(get("/admin-service/ml/dashboard/inference-stats")
                        .header("token", DoctorJwtUtil.createToken(
                                "D001", "13800000000", 2)))
                .andExpect(jsonPath("$.code").value(403));

        verifyNoInteractions(service);
    }

    @Test
    void dashboardAcceptsAdminBearerToken() throws Exception {
        when(service.getInferenceStats()).thenReturn(Map.of("todayTotal", 2));

        mockMvc.perform(get("/admin-service/ml/dashboard/inference-stats")
                        .header("Authorization", "Bearer " + DoctorJwtUtil.createToken(
                                "A001", "13800000001", 3)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.todayTotal").value(2));

        verify(service).getInferenceStats();
    }

    @Test
    void ctInferenceUploadKeepsExistingDoctorServiceProxyContractWithoutAdminToken() throws Exception {
        when(service.predictCtArtifact(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Map.of("status", "success"));

        mockMvc.perform(multipart("/admin-service/ml/inference/ct-artifact")
                        .file("file", new byte[] {1}))
                .andExpect(jsonPath("$.code").value(200));

        verify(service).predictCtArtifact(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ctInferenceDownloadKeepsExistingDoctorServiceProxyContractWithoutAdminToken() throws Exception {
        when(service.downloadCtArtifactMask("scan_mask.nii.gz"))
                .thenReturn(ResponseEntity.ok("mask".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(get("/admin-service/ml/inference/ct-artifact/result/scan_mask.nii.gz"))
                .andExpect(status().isOk());

        verify(service).downloadCtArtifactMask("scan_mask.nii.gz");
    }

    @Test
    void modelListAliasReturnsTwoBusinessModelsForFrontendContract() {
        List<Map<String, Object>> models = List.of(
                Map.of("modelKey", "ct-artifact-model", "status", "READY"),
                Map.of("modelKey", "ct-lesion-model", "status", "READY"));
        when(service.getModelList()).thenReturn(models);

        Object data = controller.modelListAlias().getData();

        assertThat(data).isEqualTo(models);
        verify(service).getModelList();
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

    @Test
    void ctInferencePreviewDownloadDelegatesPreviewFileToMlService() throws Exception {
        byte[] previewBytes = "png".getBytes(StandardCharsets.UTF_8);
        when(service.downloadCtArtifactPreview("scan_preview_z1.png"))
                .thenReturn(ResponseEntity.ok(previewBytes));

        ResponseEntity<byte[]> response = controller.downloadCtArtifactPreview("scan_preview_z1.png");

        assertThat(response.getBody()).isEqualTo(previewBytes);
        verify(service).downloadCtArtifactPreview("scan_preview_z1.png");
    }

    @Test
    void ctLesionInferenceDelegatesUploadToMlService() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "ct.nii.gz",
                "application/octet-stream", new byte[] {1});
        when(service.predictCtLesion(file)).thenReturn(Map.of("status", "success"));

        Object data = controller.predictCtLesion(file).getData();

        assertThat(data).isEqualTo(Map.of("status", "success"));
        verify(service).predictCtLesion(file);
    }

    @Test
    void ctLesionResultDownloadDelegatesMaskFileToMlService() throws Exception {
        byte[] maskBytes = "mask".getBytes(StandardCharsets.UTF_8);
        when(service.downloadCtLesionMask("scan_lesion_mask.nii.gz"))
                .thenReturn(ResponseEntity.ok(maskBytes));

        ResponseEntity<byte[]> response = controller.downloadCtLesionMask("scan_lesion_mask.nii.gz");

        assertThat(response.getBody()).isEqualTo(maskBytes);
        verify(service).downloadCtLesionMask("scan_lesion_mask.nii.gz");
    }

    @Test
    void ctLesionPreviewDownloadDelegatesPreviewFileToMlService() throws Exception {
        byte[] previewBytes = "png".getBytes(StandardCharsets.UTF_8);
        when(service.downloadCtLesionPreview("scan_lesion_preview_z1.png"))
                .thenReturn(ResponseEntity.ok(previewBytes));

        ResponseEntity<byte[]> response = controller.downloadCtLesionPreview("scan_lesion_preview_z1.png");

        assertThat(response.getBody()).isEqualTo(previewBytes);
        verify(service).downloadCtLesionPreview("scan_lesion_preview_z1.png");
    }
}

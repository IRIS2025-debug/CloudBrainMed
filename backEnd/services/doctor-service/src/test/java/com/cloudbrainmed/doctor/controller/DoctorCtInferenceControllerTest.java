package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.cloudbrainmed.doctor.feign.AiMlOpsFeignClient;
import org.junit.jupiter.api.Test;
import com.cloudbrainmed.common.exception.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DoctorCtInferenceControllerTest {

    private final AiMlOpsFeignClient aiMlOpsFeignClient =
            mock(AiMlOpsFeignClient.class);
    private final DoctorCtInferenceController controller =
            new DoctorCtInferenceController(aiMlOpsFeignClient, "internal-key");
    private final DoctorCtInferenceController controllerWithoutInternalKey =
            new DoctorCtInferenceController(aiMlOpsFeignClient, "");

    @Test
    void artifactInferenceAllowsExamDoctorAndReturnsAiResultData() throws Exception {
        String token = DoctorJwtUtil.createToken("D002", "11111111111", 2, 2);
        MockMultipartFile file = new MockMultipartFile(
                "file", "scan.nii.gz", "application/gzip", new byte[] {1});
        when(aiMlOpsFeignClient.predictCtArtifact("internal-key", file))
                .thenReturn(Result.ok(Map.of("status", "success")));

        Result<?> result = controller.predictCtArtifact(token, file);

        assertThat(result.getData()).isEqualTo(Map.of("status", "success"));
        verify(aiMlOpsFeignClient).predictCtArtifact("internal-key", file);
    }

    @Test
    void artifactInferenceRejectsReceptionDoctor() {
        String token = DoctorJwtUtil.createToken("D001", "11111111111", 2, 1);
        MockMultipartFile file = new MockMultipartFile(
                "file", "scan.nii.gz", "application/gzip", new byte[] {1});

        assertThrows(RuntimeException.class,
                () -> controller.predictCtArtifact(token, file));
    }

    @Test
    void artifactPreviewDownloadRequiresExamDoctorToken() {
        assertThrows(RuntimeException.class,
                () -> controller.downloadCtArtifactPreview("", "scan_preview_z1.png"));
    }

    @Test
    void artifactPreviewDownloadRejectsMalformedTokenAsBusinessException() {
        assertThatThrownBy(() ->
                controller.downloadCtArtifactPreview("not-a-jwt", "scan_preview_z1.png"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void lesionMaskDownloadAcceptsHeaderToken() {
        String token = DoctorJwtUtil.createToken("D002", "11111111111", 2, 2);
        byte[] maskBytes = "mask".getBytes(StandardCharsets.UTF_8);
        when(aiMlOpsFeignClient.downloadCtLesionMask("internal-key", "scan_mask.nii.gz"))
                .thenReturn(ResponseEntity.ok(maskBytes));

        ResponseEntity<byte[]> response = controller.downloadCtLesionMask(token, "scan_mask.nii.gz");

        assertThat(response.getBody()).isEqualTo(maskBytes);
        verify(aiMlOpsFeignClient).downloadCtLesionMask("internal-key", "scan_mask.nii.gz");
    }

    @Test
    void lesionMaskDownloadRejectsMissingHeaderToken() {
        assertThatThrownBy(() -> controller.downloadCtLesionMask(null, "scan_mask.nii.gz"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未登录");
    }

    @Test
    void artifactInferenceRejectsMissingInternalServiceKeyBeforeFeignCall() {
        String token = DoctorJwtUtil.createToken("D002", "11111111111", 2, 2);
        MockMultipartFile file = new MockMultipartFile(
                "file", "scan.nii.gz", "application/gzip", new byte[] {1});

        assertThatThrownBy(() -> controllerWithoutInternalKey.predictCtArtifact(token, file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("INTERNAL_SERVICE_KEY");
    }
}

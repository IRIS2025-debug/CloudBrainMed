package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.cloudbrainmed.doctor.feign.AiMlOpsFeignClient;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DoctorCtInferenceControllerTest {

    private final AiMlOpsFeignClient aiMlOpsFeignClient =
            mock(AiMlOpsFeignClient.class);
    private final DoctorCtInferenceController controller =
            new DoctorCtInferenceController(aiMlOpsFeignClient);

    @Test
    void artifactInferenceAllowsExamDoctorAndReturnsAiResultData() throws Exception {
        String token = DoctorJwtUtil.createToken("D002", "11111111111", 2, 2);
        MockMultipartFile file = new MockMultipartFile(
                "file", "scan.nii.gz", "application/gzip", new byte[] {1});
        when(aiMlOpsFeignClient.predictCtArtifact(file))
                .thenReturn(Result.ok(Map.of("status", "success")));

        Result<?> result = controller.predictCtArtifact(token, file);

        assertThat(result.getData()).isEqualTo(Map.of("status", "success"));
        verify(aiMlOpsFeignClient).predictCtArtifact(file);
    }

    @Test
    void artifactInferenceRejectsReceptionDoctor() {
        String token = DoctorJwtUtil.createToken("D001", "11111111111", 2, 1);
        MockMultipartFile file = new MockMultipartFile(
                "file", "scan.nii.gz", "application/gzip", new byte[] {1});

        assertThrows(RuntimeException.class,
                () -> controller.predictCtArtifact(token, file));
    }
}

package com.cloudbrainmed.ai.service;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface MlOpsService {
    Map<String, Object> getInferenceStats();

    Map<String, Object> getModelStats();

    Map<String, Object> getInferenceLogs(int page, int limit);

    Map<String, Object> checkPythonService();

    Map<String, Object> predictCtArtifact(MultipartFile file) throws Exception;

    ResponseEntity<byte[]> downloadCtArtifactMask(String maskFilename) throws Exception;

    ResponseEntity<byte[]> downloadCtArtifactPreview(String previewFilename) throws Exception;

    Map<String, Object> predictCtLesion(MultipartFile file) throws Exception;

    ResponseEntity<byte[]> downloadCtLesionMask(String maskFilename) throws Exception;

    ResponseEntity<byte[]> downloadCtLesionPreview(String previewFilename) throws Exception;
}

package com.cloudbrainmed.doctor.feign;

import com.cloudbrainmed.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@FeignClient(name = "ai-service", contextId = "doctorAiMlOpsFeignClient")
public interface AiMlOpsFeignClient {

    @PostMapping(
            value = "/admin-service/ml/inference/ct-artifact",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Result<Map<String, Object>> predictCtArtifact(
            @RequestPart("file") MultipartFile file);

    @PostMapping(
            value = "/admin-service/ml/inference/ct-lesion",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Result<Map<String, Object>> predictCtLesion(
            @RequestPart("file") MultipartFile file);

    @GetMapping("/admin-service/ml/inference/ct-artifact/result/{maskFilename}")
    ResponseEntity<byte[]> downloadCtArtifactMask(
            @PathVariable("maskFilename") String maskFilename);

    @GetMapping("/admin-service/ml/inference/ct-artifact/preview/{previewFilename}")
    ResponseEntity<byte[]> downloadCtArtifactPreview(
            @PathVariable("previewFilename") String previewFilename);

    @GetMapping("/admin-service/ml/inference/ct-lesion/result/{maskFilename}")
    ResponseEntity<byte[]> downloadCtLesionMask(
            @PathVariable("maskFilename") String maskFilename);

    @GetMapping("/admin-service/ml/inference/ct-lesion/preview/{previewFilename}")
    ResponseEntity<byte[]> downloadCtLesionPreview(
            @PathVariable("previewFilename") String previewFilename);
}

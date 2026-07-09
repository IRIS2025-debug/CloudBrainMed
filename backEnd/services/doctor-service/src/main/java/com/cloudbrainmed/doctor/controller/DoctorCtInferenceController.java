package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.cloudbrainmed.doctor.feign.AiMlOpsFeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/doctor-service/exam")
public class DoctorCtInferenceController {

    private final AiMlOpsFeignClient aiMlOpsFeignClient;

    public DoctorCtInferenceController(AiMlOpsFeignClient aiMlOpsFeignClient) {
        this.aiMlOpsFeignClient = aiMlOpsFeignClient;
    }

    @PostMapping("/ct-artifact")
    public Result<?> predictCtArtifact(
            @RequestHeader(value = "token", required = false) String token,
            @RequestParam("file") MultipartFile file) {
        assertExamDoctor(token);
        return unwrap(aiMlOpsFeignClient.predictCtArtifact(file));
    }

    @PostMapping("/ct-lesion")
    public Result<?> predictCtLesion(
            @RequestHeader(value = "token", required = false) String token,
            @RequestParam("file") MultipartFile file) {
        assertExamDoctor(token);
        return unwrap(aiMlOpsFeignClient.predictCtLesion(file));
    }

    @GetMapping("/ct-artifact/result/{maskFilename}")
    public ResponseEntity<byte[]> downloadCtArtifactMask(
            @PathVariable String maskFilename) {
        return aiMlOpsFeignClient.downloadCtArtifactMask(maskFilename);
    }

    @GetMapping("/ct-artifact/preview/{previewFilename}")
    public ResponseEntity<byte[]> downloadCtArtifactPreview(
            @PathVariable String previewFilename) {
        return aiMlOpsFeignClient.downloadCtArtifactPreview(previewFilename);
    }

    @GetMapping("/ct-lesion/result/{maskFilename}")
    public ResponseEntity<byte[]> downloadCtLesionMask(
            @PathVariable String maskFilename) {
        return aiMlOpsFeignClient.downloadCtLesionMask(maskFilename);
    }

    @GetMapping("/ct-lesion/preview/{previewFilename}")
    public ResponseEntity<byte[]> downloadCtLesionPreview(
            @PathVariable String previewFilename) {
        return aiMlOpsFeignClient.downloadCtLesionPreview(previewFilename);
    }

    private Result<?> unwrap(Result<Map<String, Object>> result) {
        if (result == null || result.getData() == null) {
            throw new BusinessException("CT inference service unavailable");
        }
        return Result.ok(result.getData());
    }

    private void assertExamDoctor(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("未登录，请先登录");
        }
        Integer roleType = DoctorJwtUtil.getRoleType(token);
        Integer doctorType = DoctorJwtUtil.getDoctorType(token);
        if (!Integer.valueOf(2).equals(roleType)
                || !Integer.valueOf(2).equals(doctorType)) {
            throw new BusinessException("仅检查医生可访问CT推理功能");
        }
    }
}

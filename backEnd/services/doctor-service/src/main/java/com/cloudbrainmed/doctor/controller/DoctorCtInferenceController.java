package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.cloudbrainmed.doctor.feign.AiMlOpsFeignClient;
import org.springframework.beans.factory.annotation.Value;
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
    private final String internalServiceKey;

    public DoctorCtInferenceController(
            AiMlOpsFeignClient aiMlOpsFeignClient,
            @Value("${internal.service-key:}") String internalServiceKey) {
        this.aiMlOpsFeignClient = aiMlOpsFeignClient;
        this.internalServiceKey = internalServiceKey;
    }

    @PostMapping("/ct-artifact")
    public Result<?> predictCtArtifact(
            @RequestHeader(value = "token", required = false) String token,
            @RequestParam("file") MultipartFile file) {
        assertExamDoctor(token);
        requireInternalServiceKey();
        return unwrap(aiMlOpsFeignClient.predictCtArtifact(internalServiceKey, file));
    }

    @PostMapping("/ct-lesion")
    public Result<?> predictCtLesion(
            @RequestHeader(value = "token", required = false) String token,
            @RequestParam("file") MultipartFile file) {
        assertExamDoctor(token);
        requireInternalServiceKey();
        return unwrap(aiMlOpsFeignClient.predictCtLesion(internalServiceKey, file));
    }

    @GetMapping("/ct-artifact/result/{maskFilename}")
    public ResponseEntity<byte[]> downloadCtArtifactMask(
            @RequestHeader(value = "token", required = false) String token,
            @PathVariable String maskFilename) {
        assertExamDoctor(token);
        requireInternalServiceKey();
        return aiMlOpsFeignClient.downloadCtArtifactMask(internalServiceKey, maskFilename);
    }

    @GetMapping("/ct-artifact/preview/{previewFilename}")
    public ResponseEntity<byte[]> downloadCtArtifactPreview(
            @RequestHeader(value = "token", required = false) String token,
            @PathVariable String previewFilename) {
        assertExamDoctor(token);
        requireInternalServiceKey();
        return aiMlOpsFeignClient.downloadCtArtifactPreview(internalServiceKey, previewFilename);
    }

    @GetMapping("/ct-lesion/result/{maskFilename}")
    public ResponseEntity<byte[]> downloadCtLesionMask(
            @RequestHeader(value = "token", required = false) String token,
            @PathVariable String maskFilename) {
        assertExamDoctor(token);
        requireInternalServiceKey();
        return aiMlOpsFeignClient.downloadCtLesionMask(internalServiceKey, maskFilename);
    }

    @GetMapping("/ct-lesion/preview/{previewFilename}")
    public ResponseEntity<byte[]> downloadCtLesionPreview(
            @RequestHeader(value = "token", required = false) String token,
            @PathVariable String previewFilename) {
        assertExamDoctor(token);
        requireInternalServiceKey();
        return aiMlOpsFeignClient.downloadCtLesionPreview(internalServiceKey, previewFilename);
    }

    private Result<?> unwrap(Result<Map<String, Object>> result) {
        if (result == null || result.getData() == null) {
            throw new BusinessException("CT inference service unavailable");
        }
        return Result.ok(result.getData());
    }

    private void requireInternalServiceKey() {
        if (internalServiceKey == null || internalServiceKey.isBlank()) {
            throw new BusinessException("未配置INTERNAL_SERVICE_KEY，CT推理代理不可用");
        }
    }

    private void assertExamDoctor(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("未登录，请先登录");
        }
        try {
            Integer roleType = DoctorJwtUtil.getRoleType(token);
            Integer doctorType = DoctorJwtUtil.getDoctorType(token);
            if (!Integer.valueOf(2).equals(roleType)
                    || !Integer.valueOf(2).equals(doctorType)) {
                throw new BusinessException("仅检查医生可访问CT推理功能");
            }
        } catch (BusinessException businessException) {
            throw businessException;
        } catch (Exception exception) {
            throw new BusinessException("医生登录凭证无效");
        }
    }
}

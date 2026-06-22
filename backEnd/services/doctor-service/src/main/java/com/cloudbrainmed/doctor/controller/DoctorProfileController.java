package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.cloudbrainmed.doctor.service.DoctorService;
import com.cloudbrainmed.doctor.vo.DoctorProfileVo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/doctor-service/profile")
public class DoctorProfileController {

    @Resource
    private DoctorService doctorService;

    @GetMapping("/info")
    public Result<DoctorProfileVo> info(@RequestHeader(value = "token", required = false) String token) {
        String doctorId = extractDoctorId(token);
        DoctorProfileVo vo = doctorService.getDoctorInfo(doctorId);
        return Result.ok(vo);
    }

    @PutMapping("/update")
    public Result<?> update(@RequestHeader(value = "token", required = false) String token,
                            @RequestBody Map<String, String> body) {
        String doctorId = extractDoctorId(token);
        doctorService.updateDoctorProfile(
                doctorId,
                body.get("goodAt"),
                body.get("introduction"),
                body.get("email")
        );
        return Result.ok();
    }

    @PostMapping("/avatar-upload")
    public Result<?> avatarUpload(@RequestHeader(value = "token", required = false) String token,
                                   @RequestParam("file") MultipartFile file) {
        // TODO: 接入 OSS/本地文件存储后实现真正上传
        return Result.ok(Map.of("avatarUrl", "/files/avatar/default.png"));
    }

    @PostMapping("/change-phone")
    public Result<?> changePhone(@RequestHeader(value = "token", required = false) String token,
                                  @RequestBody Map<String, String> body) {
        return Result.ok();
    }

    @PostMapping("/change-password")
    public Result<?> changePassword(@RequestHeader(value = "token", required = false) String token,
                                     @RequestBody Map<String, String> body) {
        // TODO: 验证旧密码 → 更新新密码
        return Result.ok();
    }

    /** 查询医生信息完善状态（首次登录引导） */
    @GetMapping("/setup-status")
    public Result<?> setupStatus(@RequestHeader(value = "token", required = false) String token) {
        String doctorId = extractDoctorId(token);
        DoctorProfileVo vo = doctorService.getDoctorInfo(doctorId);
        boolean needSetup = vo.getIntroduction() == null || vo.getIntroduction().isBlank()
                || vo.getGoodAt() == null || vo.getGoodAt().isBlank();
        return Result.ok(Map.of("needSetup", needSetup));
    }

    /**
     * 从 header 或 JWT 中提取 doctorId。
     * 优先从 token header 直接读取（兼容旧版直传 doctorId）；
     * 否则用 DoctorJwtUtil 解析 JWT 的 userId claim。
     */
    private String extractDoctorId(String token) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("未登录，请先登录");
        }
        try {
            return DoctorJwtUtil.getUserId(token);
        } catch (Exception e) {
            // 兼容直接传 doctorId 的场景
            return token;
        }
    }
}
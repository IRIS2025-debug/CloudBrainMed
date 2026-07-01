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
                                  @RequestParam("file") MultipartFile file) throws java.io.IOException {
        String doctorId = extractDoctorId(token);
        String avatarUrl = doctorService.uploadAvatar(doctorId, file.getBytes(), file.getOriginalFilename());
        return Result.ok(Map.of("avatarUrl", avatarUrl));
    }

    @PostMapping("/change-phone")
    public Result<?> changePhone(@RequestHeader(value = "token", required = false) String token,
                                 @RequestBody Map<String, String> body) {
        String doctorId = extractDoctorId(token);
        doctorService.changePhone(doctorId, body.get("oldPhone"), body.get("newPhone"));
        return Result.ok();
    }

    @PostMapping("/change-password")
    public Result<?> changePassword(@RequestHeader(value = "token", required = false) String token,
                                    @RequestBody Map<String, String> body) {
        String doctorId = extractDoctorId(token);
        doctorService.changePassword(doctorId, body.get("oldPassword"), body.get("newPassword"));
        return Result.ok();
    }

    @GetMapping("/setup-status")
    public Result<?> setupStatus(@RequestHeader(value = "token", required = false) String token) {
        String doctorId = extractDoctorId(token);
        DoctorProfileVo vo = doctorService.getDoctorInfo(doctorId);
        boolean needSetup = vo.getIntroduction() == null || vo.getIntroduction().isBlank()
                || vo.getGoodAt() == null || vo.getGoodAt().isBlank();
        return Result.ok(Map.of("needSetup", needSetup));
    }

    private String extractDoctorId(String token) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("未登录，请先登录");
        }
        Integer roleType;
        String doctorId;
        try {
            roleType = DoctorJwtUtil.getRoleType(token);
            doctorId = DoctorJwtUtil.getUserId(token);
        } catch (Exception e) {
            throw new RuntimeException("医生登录凭证无效");
        }
        if (!Integer.valueOf(2).equals(roleType)) {
            throw new RuntimeException("仅医生可访问医生资料");
        }
        return doctorId;
    }
}

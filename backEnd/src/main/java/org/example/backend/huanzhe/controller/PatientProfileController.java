package org.example.backend.huanzhe.controller;

import org.example.backend.common.Result;
import org.example.backend.huanzhe.service.PatientProfileService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/patient/profile")
public class PatientProfileController {

    private final PatientProfileService service;

    public PatientProfileController(PatientProfileService service) {
        this.service = service;
    }

    /** 4.3.1.1 查询个人脱敏信息 */
    @GetMapping("/info")
    public Result<?> info(@RequestHeader(value = "token", required = false) String token) {
        String patientId = extractPatientId(token);
        return Result.ok(service.getInfo(patientId));
    }

    /** 4.3.1.2 更新基础信息（姓名、地址）。性别和生日由身份证自动推导，不通过本接口修改 */
    @PutMapping("/update")
    public Result<?> update(@RequestHeader(value = "token", required = false) String token,
                            @RequestBody Map<String, String> body) {
        String patientId = extractPatientId(token);
        service.updateInfo(patientId,
                body.get("name"), body.get("address"));
        return Result.ok();
    }

    /** 4.3.1.3 头像上传 */
    @PostMapping("/avatar-upload")
    public Result<?> avatarUpload(@RequestHeader(value = "token", required = false) String token,
                                  @RequestParam("file") MultipartFile file) throws IOException {
        String patientId = extractPatientId(token);
        String url = service.uploadAvatar(patientId, file.getBytes(), file.getOriginalFilename());
        return Result.ok(Map.of("avatarUrl", url));
    }

    /** 4.3.1.4 修改手机号 */
    @PostMapping("/change-phone")
    public Result<?> changePhone(@RequestHeader(value = "token", required = false) String token,
                                 @RequestBody Map<String, String> body) {
        String patientId = extractPatientId(token);
        service.changePhone(patientId,
                body.get("oldPhone"), body.get("newPhone"), body.get("smsCode"));
        return Result.ok();
    }

    /** 4.3.1.5 修改登录密码 */
    @PostMapping("/change-password")
    public Result<?> changePassword(@RequestHeader(value = "token", required = false) String token,
                                    @RequestBody Map<String, String> body) {
        String patientId = extractPatientId(token);
        service.changePassword(patientId,
                body.get("oldPassword"), body.get("newPassword"));
        return Result.ok();
    }

    /** 4.3.1.6 身份证修改权限验证 */
    @PostMapping("/verify-idcard")
    public Result<?> verifyIdCard(@RequestHeader(value = "token", required = false) String token,
                                  @RequestBody Map<String, String> body) {
        String patientId = extractPatientId(token);
        service.verifyIdCard(patientId, body.get("password"));
        return Result.ok();
    }

    private String extractPatientId(String token) {
        // TODO: 解析 JWT token 获取 patientId
        return "P001";
    }
}

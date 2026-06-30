package com.cloudbrainmed.patient.controller;

import com.cloudbrainmed.api.feign.PaymentFeignClient;
import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.JwtUtil;
import com.cloudbrainmed.patient.module5.service.PatientProfileService;
import com.cloudbrainmed.patient.service.RegisterService;
import com.cloudbrainmed.payment.vo.PayResultVo;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 患者移动端个人中心控制器
 * 对应鸿蒙端页面：患者个人信息 + 挂号记录 + 缴费记录
 */
@RestController
@RequestMapping("/patient-service/profile")
public class PatientMobileProfileController {

    private final PatientProfileService profileService;
    private final RegisterService registerService;
    private final PaymentFeignClient paymentFeignClient;
    private final JwtUtil jwtUtil;

    public PatientMobileProfileController(PatientProfileService profileService,
                                           RegisterService registerService,
                                           PaymentFeignClient paymentFeignClient,
                                           JwtUtil jwtUtil) {
        this.profileService = profileService;
        this.registerService = registerService;
        this.paymentFeignClient = paymentFeignClient;
        this.jwtUtil = jwtUtil;
    }

    // ==================== 4.3.1 个人信息 ====================

    /** 4.3.1.1 查询个人脱敏信息 */
    @GetMapping("/info")
    public Result<?> info(@RequestHeader(value = "token", required = false) String token) {
        String patientId = extractPatientId(token);
        return Result.ok(profileService.getInfo(patientId));
    }

    /** 4.3.1.1+ 查询个人完整信息（不脱敏，仅用于改手机号等场景）。警告：新增 Patient 敏感字段时需确认本接口是否需要跳过该字段 */
    @GetMapping("/info-raw")
    public Result<?> infoRaw(@RequestHeader(value = "token", required = false) String token) {
        String patientId = extractPatientId(token);
        return Result.ok(profileService.getInfoRaw(patientId));
    }

    /** 4.3.1.2 更新基础信息 */
    @PutMapping("/update")
    public Result<?> update(@RequestHeader(value = "token", required = false) String token,
                            @RequestBody Map<String, String> body) {
        String patientId = extractPatientId(token);
        profileService.updateInfo(patientId,
                body.get("name"), body.get("gender"),
                body.get("address"), body.get("birthday"));
        return Result.ok();
    }

    /** 4.3.1.3 头像上传 */
    @PostMapping("/avatar-upload")
    public Result<?> avatarUpload(@RequestHeader(value = "token", required = false) String token,
                                  @RequestParam("file") MultipartFile file) throws IOException {
        String patientId = extractPatientId(token);
        String url = profileService.uploadAvatar(patientId, file.getBytes(), file.getOriginalFilename());
        return Result.ok(Map.of("avatarUrl", url));
    }

    /** 4.3.1.4 修改手机号 */
    @PostMapping("/change-phone")
    public Result<?> changePhone(@RequestHeader(value = "token", required = false) String token,
                                 @RequestBody Map<String, String> body) {
        String patientId = extractPatientId(token);
        profileService.changePhone(patientId,
                body.get("oldPhone"), body.get("newPhone"), body.get("smsCode"));
        return Result.ok();
    }

    /** 4.3.1.5 修改登录密码 */
    @PostMapping("/change-password")
    public Result<?> changePassword(@RequestHeader(value = "token", required = false) String token,
                                    @RequestBody Map<String, String> body) {
        String patientId = extractPatientId(token);
        profileService.changePassword(patientId,
                body.get("oldPassword"), body.get("newPassword"));
        return Result.ok();
    }

    /** 4.3.1.6 身份证修改权限验证 */
    @PostMapping("/verify-idcard")
    public Result<?> verifyIdCard(@RequestHeader(value = "token", required = false) String token,
                                  @RequestBody Map<String, String> body) {
        String patientId = extractPatientId(token);
        profileService.verifyIdCard(patientId, body.get("password"));
        return Result.ok();
    }

    /** 4.3.1.7 修改身份证号（内置密码校验，无需先调 /verify-idcard） */
    @PostMapping("/change-idcard")
    public Result<?> changeIdCard(@RequestHeader(value = "token", required = false) String token,
                                  @RequestBody Map<String, String> body) {
        String patientId = extractPatientId(token);
        profileService.changeIdCard(patientId, body.get("idCard"), body.get("password"));
        return Result.ok();
    }

    // ==================== 4.3.2 挂号记录 ====================

    /** 查询患者挂号记录列表 */
    @GetMapping("/registers")
    public Result<?> registers(@RequestHeader(value = "token", required = false) String token) {
        String patientId = extractPatientId(token);
        return Result.ok(registerService.getRegisterHistory(patientId));
    }

    // ==================== 4.3.3 缴费记录 ====================

    /** 查询患者缴费记录（通过 Feign → payment-service） */
    @GetMapping("/payments")
    public Result<List<PayResultVo>> payments(@RequestHeader(value = "token", required = false) String token) {
        String patientId = extractPatientId(token);
        return paymentFeignClient.getPayHistory(patientId);
    }

    // ==================== 4.3.4 聚合接口（鸿蒙端一次拉取全部数据） ====================

    /**
     * 患者个人中心完整数据（个人信息 + 挂号记录 + 缴费记录）
     * 鸿蒙端首页调用此接口即可获取全部模块数据
     */
    @GetMapping("/full")
    public Result<?> full(@RequestHeader(value = "token", required = false) String token) {
        String patientId = extractPatientId(token);

        Map<String, Object> data = new HashMap<>();
        // 板块1: 患者个人信息（脱敏）
        data.put("profile", profileService.getInfo(patientId));
        // 板块2: 挂号记录
        data.put("registers", registerService.getRegisterHistory(patientId));
        // 板块3: 缴费记录
        Result<List<PayResultVo>> paymentResult = paymentFeignClient.getPayHistory(patientId);
        data.put("payments", paymentResult.getData());

        return Result.ok(data);
    }

    // ==================== 工具方法 ====================

    /**
     * 从 JWT token 中提取 patientId
     */
    private String extractPatientId(String token) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("未登录，请先登录");
        }
        try {
            return jwtUtil.getPatientIdFromToken(token);
        } catch (Exception e) {
            // 兼容开发阶段直接传 patientId
            if (token.startsWith("P") && token.length() < 30) {
                return token;
            }
            throw new RuntimeException("Token 无效，请重新登录");
        }
    }
}

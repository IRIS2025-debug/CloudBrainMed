package com.cloudbrainmed.auth.controller;

import com.cloudbrainmed.auth.dto.authLoginDto;
import com.cloudbrainmed.auth.dto.authRegisterDto;
import com.cloudbrainmed.auth.service.AuthService;
import com.cloudbrainmed.auth.vo.AuthLoginVo;
import com.cloudbrainmed.auth.vo.PatientInfoVo;
import com.cloudbrainmed.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth-service")
@Validated
public class PatientAuthController {

    @Autowired
    private AuthService authService;

    @Value("${spring.profiles.active:production}")
    private String activeProfile;

    /**
     * 患者移动端注册
     */
    @PostMapping("/patient/register")
    public Result<PatientInfoVo> register(@Valid @RequestBody authRegisterDto registerDTO) {
        if (authService.checkPhoneExists(registerDTO.getPhone())) {
            return Result.error(400, "手机号已注册");
        }

        if (authService.checkIdCardExists(registerDTO.getIdCard())) {
            return Result.error(400, "身份证已注册");
        }

        PatientInfoVo patientInfo = authService.register(registerDTO);
        if (patientInfo == null) {
            return Result.error(500, "注册失败，请稍后重试");
        }

        return Result.success("注册成功", patientInfo);
    }

    /**
     * 患者移动端登录
     */
    @PostMapping("/patient/login")
    public Result<AuthLoginVo> login(@Valid @RequestBody authLoginDto loginDTO) {
        AuthLoginVo loginResult = authService.login(loginDTO);

        if (loginResult == null) {
            return Result.error(401, "手机号或验证码/密码错误");
        }

        return Result.success("登录成功", loginResult);
    }

    /**
     * 发送验证码 - 支持JSON请求体
     * skipRegisterCheck=true 时跳过注册检查（改手机号场景，新号码可能未注册）
     */
    @PostMapping("/patient/send-code")
    public Result<Map<String, String>> sendVerifyCode(@RequestBody Map<String, String> request) {
        // 从请求体中获取手机号
        String phone = request.get("phone");
        boolean skipRegisterCheck = "true".equals(request.get("skipRegisterCheck"));

        // 1. 检查手机号是否为空
        if (phone == null || phone.trim().isEmpty()) {
            return Result.error(400, "手机号不能为空");
        }

        // 2. 检查手机号格式
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            return Result.error(400, "手机号格式不正确");
        }

        // 3. 检查手机号是否已注册（改手机号场景可跳过）
        if (!skipRegisterCheck && !authService.checkPhoneExists(phone)) {
            return Result.error(400, "该手机号未注册");
        }
        // 改手机号场景：新手机号必须未被注册
        if (skipRegisterCheck && authService.checkPhoneExists(phone)) {
            return Result.error(400, "该手机号已被注册，无法使用");
        }

        // 4. 发送验证码（统一调用，返回验证码）
        String code = authService.sendVerifyCode(phone, skipRegisterCheck);
        if (code == null) {
            return Result.error(429, "发送过于频繁，请稍后再试");
        }

        // 5. 构建响应
        Map<String, String> data = new HashMap<>();
        data.put("message", "验证码已发送，请注意查收");

        // 判断是否开发/测试环境，返回验证码
        boolean isDevOrTest = "dev".equals(activeProfile) || "test".equals(activeProfile);
        if (isDevOrTest) {
            data.put("verifyCode", code);  // 仅在开发/测试环境返回
        }

        return Result.success("验证码发送成功", data);
    }

    /**
     * 校验验证码（供其他微服务调用）
     */
    @PostMapping("/patient/verify-code")
    public Result<Map<String, Object>> verifyCode(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String code = request.get("code");

        if (phone == null || phone.trim().isEmpty()) {
            return Result.error(400, "手机号不能为空");
        }
        if (code == null || code.trim().isEmpty()) {
            return Result.error(400, "验证码不能为空");
        }

        boolean valid = authService.verifyCode(phone, code);
        Map<String, Object> data = new HashMap<>();
        data.put("valid", valid);
        return Result.success(data);
    }
}
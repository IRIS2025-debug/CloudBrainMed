package com.cloudbrainmed.auth.controller;

import com.cloudbrainmed.auth.dto.authLoginDto;
import com.cloudbrainmed.auth.dto.authRegisterDto;
import com.cloudbrainmed.auth.service.AuthService;
import com.cloudbrainmed.auth.vo.AuthLoginVo;
import com.cloudbrainmed.auth.vo.PatientInfoVo;
import com.cloudbrainmed.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
     */
    @PostMapping("/patient/send-code")
    public Result<Map<String, String>> sendVerifyCode(@RequestBody Map<String, String> request) {
        // 从请求体中获取手机号
        String phone = request.get("phone");

        // 1. 检查手机号是否为空
        if (phone == null || phone.trim().isEmpty()) {
            return Result.error(400, "手机号不能为空");
        }

        // 2. 检查手机号格式
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            return Result.error(400, "手机号格式不正确");
        }

        // 3. 检查手机号是否已注册
        if (!authService.checkPhoneExists(phone)) {
            return Result.error(400, "该手机号未注册");
        }

        // 4. 发送验证码
        boolean success = authService.sendVerifyCode(phone);
        if (!success) {
            return Result.error(429, "发送过于频繁，请稍后再试");
        }

        // 5. 返回结果
        Map<String, String> data = new HashMap<>();
        data.put("message", "验证码已发送，请注意查收");

        return Result.success("验证码发送成功", data);
    }
}
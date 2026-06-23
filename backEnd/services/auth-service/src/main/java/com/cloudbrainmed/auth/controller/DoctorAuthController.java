package com.cloudbrainmed.auth.controller;

import com.cloudbrainmed.auth.service.DoctorAuthService;
import org.springframework.validation.annotation.Validated;
import com.cloudbrainmed.auth.dto.LoginDto;
import com.cloudbrainmed.auth.service.AuthService;
import com.cloudbrainmed.common.result.Result;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
@Data
@RestController
@RequestMapping("/auth-service")
@Validated
public class DoctorAuthController {
    @Autowired
    private DoctorAuthService doctorAuthService;

    @PostMapping("/login")
    public Result login(@RequestBody LoginDto dto) {
        String token = doctorAuthService.login(dto);
        Map<String, Object> map = new HashMap<>();
        map.put("token", token);
        map.put("roleType", dto.getRoleType());
        return Result.success(map);
    }
}
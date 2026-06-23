package com.cloudbrainmed.auth.service;

import com.cloudbrainmed.auth.dto.LoginDto;

import java.util.Map;

public interface DoctorAuthService {
     Map<String, Object> login(LoginDto dto);
}
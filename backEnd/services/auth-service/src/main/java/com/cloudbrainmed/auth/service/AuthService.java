package com.cloudbrainmed.auth.service;


import com.cloudbrainmed.auth.dto.authLoginDto;
import com.cloudbrainmed.auth.dto.authRegisterDto;
import com.cloudbrainmed.auth.vo.AuthLoginVo;
import com.cloudbrainmed.auth.vo.PatientInfoVo;

public interface AuthService {

    /**
     * 患者注册
     */
    PatientInfoVo register(authRegisterDto registerDTO);

    /**
     * 患者登录
     */
    AuthLoginVo login(authLoginDto loginDTO);

    /**
     * 检查手机号是否存在
     */
    boolean checkPhoneExists(String phone);

    /**
     * 检查身份证是否存在
     */
    boolean checkIdCardExists(String idCard);

    /**
     * 发送验证码
     * @param phone 手机号
     * @param skipRegisterCheck 是否跳过注册检查（改手机号场景需要跳过，因为新号码尚未注册）
     * @return 是否发送成功
     */
    boolean sendVerifyCode(String phone, boolean skipRegisterCheck);

    /**
     * 验证验证码
     * @param phone 手机号
     * @param code 验证码
     * @return 是否验证成功
     */
    boolean verifyCode(String phone, String code);
}
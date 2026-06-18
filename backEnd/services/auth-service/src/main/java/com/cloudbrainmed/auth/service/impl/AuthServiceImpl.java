package com.cloudbrainmed.auth.service.impl;

import com.cloudbrainmed.auth.dto.authLoginDto;
import com.cloudbrainmed.auth.dto.authRegisterDto;
import com.cloudbrainmed.auth.mapper.PatientUserMapper;
import com.cloudbrainmed.auth.service.AuthService;
import com.cloudbrainmed.common.utils.JwtUtil;
import com.cloudbrainmed.auth.vo.AuthLoginVo;
import com.cloudbrainmed.auth.vo.PatientInfoVo;
import com.cloudbrainmed.patient.entity.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class AuthServiceImpl implements AuthService {
    @Autowired
    PatientUserMapper patientUserMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String SALT = "cloudbrainmed_salt";

    // Redis Key前缀
    private static final String VERIFY_CODE_PREFIX = "verify_code:";
    // 验证码有效期（秒）
    private static final long CODE_EXPIRE_TIME = 300; // 5分钟
    // 验证码发送频率限制Key前缀
    private static final String CODE_SEND_LIMIT_PREFIX = "verify_code_limit:";
    // 发送频率限制时间（秒）
    private static final long SEND_LIMIT_TIME = 60; // 1分钟

    @Override
    @Transactional
    public PatientInfoVo register(authRegisterDto registerDTO) {
        Patient patient = new Patient();
        patient.setPatientId(generatePatientId());
        patient.setName(registerDTO.getName());
        patient.setGender(registerDTO.getGender());
        patient.setPhone(registerDTO.getPhone());
        patient.setIdCard(registerDTO.getIdCard());
        patient.setAddress(registerDTO.getAddress());
        LocalDate birthday = registerDTO.getBirthdayFromIdCard();
        patient.setBirthday(birthday);
        patient.setPassword(encryptPassword(registerDTO.getPassword()));
        patient.setCreateTime(LocalDateTime.now());
        patient.setUpdateTime(LocalDateTime.now());
        patient.setIsDeleted(0);

        int result = patientUserMapper.insert(patient);
        if (result <= 0) {
            return null;
        }

        return convertToVO(patient);
    }

    @Override
    public AuthLoginVo login(authLoginDto loginDTO) {
        // 1. 参数验证
        if (loginDTO.getPhone() == null || loginDTO.getPhone().trim().isEmpty()) {
            return null;
        }

        Integer loginType = loginDTO.getLoginType();
        if (loginType == null) {
            loginType = 1; // 默认密码登录
        }

        // 2. 根据手机号查询患者
        Patient patient = patientUserMapper.selectByPhone(loginDTO.getPhone());
        if (patient == null) {
            return null;
        }

        // 3. 根据不同登录类型验证
        boolean verified = false;

        if (loginType == 1) {
            // 密码登录验证
            if (loginDTO.getPassword() == null || loginDTO.getPassword().trim().isEmpty()) {
                return null;
            }
            String encryptedPassword = encryptPassword(loginDTO.getPassword());
            verified = encryptedPassword.equals(patient.getPassword());
        } else if (loginType == 2) {
            // 验证码登录验证
            if (loginDTO.getVerifyCode() == null || loginDTO.getVerifyCode().trim().isEmpty()) {
                return null;
            }
            verified = verifyCode(loginDTO.getPhone(), loginDTO.getVerifyCode());
        } else {
            return null;
        }

        if (!verified) {
            return null;
        }

        // 4. 更新最后登录时间
        patientUserMapper.updateLastLoginTime(patient.getPatientId());

        // 5. 生成JWT token
        String token = jwtUtil.generateToken(patient.getPatientId(), patient.getPhone());
        Long expireTime = jwtUtil.getExpirationTime();

        // 6. 返回登录VO
        AuthLoginVo loginVO = new AuthLoginVo();
        loginVO.setPatientId(patient.getPatientId());
        loginVO.setName(patient.getName());
        loginVO.setPhone(patient.getPhone());
        loginVO.setToken(token);
        loginVO.setExpireTime(expireTime);
        loginVO.setGender(patient.getGender());
        if (patient.getGender() != null) {
            if (patient.getGender() == 1) {
                loginVO.setGenderText("男");
            } else if (patient.getGender() == 2) {
                loginVO.setGenderText("女");
            } else {
                loginVO.setGenderText("未知");
            }
        } else {
            loginVO.setGenderText("未知");
        }

        // 计算年龄
        if (patient.getBirthday() != null) {
            LocalDate birthday = patient.getBirthday();
            LocalDate now = LocalDate.now();
            int age = now.getYear() - birthday.getYear();
            // 如果生日还没过，年龄减1
            if (now.getMonthValue() < birthday.getMonthValue() ||
                    (now.getMonthValue() == birthday.getMonthValue() && now.getDayOfMonth() < birthday.getDayOfMonth())) {
                age--;
            }
            loginVO.setAge(age);
        } else {
            loginVO.setAge(0);
        }

        return loginVO;
    }

    /**
     * 发送验证码（仿真模式）
     * @param phone 手机号
     * @return 验证码（前端模拟使用）
     */
    @Override
    public boolean sendVerifyCode(String phone) {
        // 1. 检查手机号是否存在
        Patient patient = patientUserMapper.selectByPhone(phone);
        if (patient == null) {
            return false; // 手机号未注册
        }

        // 2. 检查发送频率限制（防止恶意刷验证码）
        String limitKey = CODE_SEND_LIMIT_PREFIX + phone;
        Boolean hasLimit = redisTemplate.hasKey(limitKey);
        if (Boolean.TRUE.equals(hasLimit)) {
            return false; // 发送太频繁
        }

        // 3. 生成6位随机验证码
        String code = String.format("%06d", new Random().nextInt(999999));
        // 如果生成的验证码不足6位，补0
        while (code.length() < 6) {
            code = "0" + code;
        }

        // 4. 存储验证码到Redis，设置过期时间
        String codeKey = VERIFY_CODE_PREFIX + phone;
        redisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRE_TIME, TimeUnit.SECONDS);

        // 5. 设置发送频率限制，60秒内不能重复发送
        redisTemplate.opsForValue().set(limitKey, "1", SEND_LIMIT_TIME, TimeUnit.SECONDS);

        // 6. 实际生产环境应调用短信服务发送验证码
        // 这里使用控制台打印模拟
        System.out.println("========== 验证码 ==========");
        System.out.println("手机号: " + phone);
        System.out.println("验证码: " + code);
        System.out.println("有效期: " + CODE_EXPIRE_TIME + "秒");
        System.out.println("============================");

        return true;
    }

    /**
     * 验证验证码
     */
    @Override
    public boolean verifyCode(String phone, String inputCode) {
        if (phone == null || inputCode == null) {
            return false;
        }

        String codeKey = VERIFY_CODE_PREFIX + phone;
        String storedCode = redisTemplate.opsForValue().get(codeKey);

        if (storedCode == null) {
            return false; // 验证码不存在或已过期
        }

        // 验证码比对
        boolean valid = storedCode.equals(inputCode);
        if (valid) {
            // 验证成功后删除验证码（一次性使用）
            redisTemplate.delete(codeKey);
        }
        return valid;
    }

    @Override
    public boolean checkPhoneExists(String phone) {
        return patientUserMapper.selectByPhone(phone) != null;
    }

    @Override
    public boolean checkIdCardExists(String idCard) {
        return patientUserMapper.selectByIdCard(idCard) != null;
    }

    // ========== 私有辅助方法 ==========

    private String generatePatientId() {
        String latestId = patientUserMapper.getLatestPatientId();

        int nextNum = 1;
        if (latestId != null && latestId.startsWith("p")) {
            try {
                String numStr = latestId.substring(1);
                nextNum = Integer.parseInt(numStr) + 1;
            } catch (NumberFormatException e) {
                nextNum = 1;
            }
        }

        return String.format("p%03d", nextNum);
    }

    private String encryptPassword(String password) {
        return DigestUtils.md5DigestAsHex(password.getBytes());
    }

    private PatientInfoVo convertToVO(Patient patient) {
        PatientInfoVo vo = new PatientInfoVo();
        vo.setPatientId(patient.getPatientId());
        vo.setName(patient.getName());

        if (patient.getGender() != null) {
            if (patient.getGender() == 1) {
                vo.setGenderText("男");
            } else if (patient.getGender() == 2) {
                vo.setGenderText("女");
            } else {
                vo.setGenderText("未知");
            }
        }

        vo.setPhone(patient.getPhone());
        vo.setAddress(patient.getAddress());
        vo.setCreateTime(patient.getCreateTime());

        if (patient.getBirthday() != null) {
            vo.calculateAge(patient.getBirthday());
        }

        return vo;
    }
}
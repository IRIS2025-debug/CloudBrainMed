package com.cloudbrainmed.auth.vo;

/**
 * 登录响应VO
 */
public class AuthLoginVo {
    private String patientId;
    private String name;
    private String phone;
    private String token;
    private Long expireTime;
    private Integer gender;      // 新增
    private String genderText;   // 新增
    private Integer age;         // 新增

    // Getters and Setters
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getExpireTime() { return expireTime; }
    public void setExpireTime(Long expireTime) { this.expireTime = expireTime; }

    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }

    public String getGenderText() { return genderText; }
    public void setGenderText(String genderText) { this.genderText = genderText; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
}
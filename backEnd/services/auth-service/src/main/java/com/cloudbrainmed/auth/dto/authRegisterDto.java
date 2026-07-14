package com.cloudbrainmed.auth.dto;

//注册登录
import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * 注册请求DTO
 * 生日将从身份证号自动计算，性别也将从身份证号自动识别
 */
public class authRegisterDto {

    @NotBlank(message = "姓名不能为空")
    @Size(min = 2, max = 20, message = "姓名长度必须在2-20之间")
    private String name;

    // 性别不再需要前端传递，由后端根据身份证号自动识别
    private Integer gender;  // 后端会自动填充

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$",
            message = "身份证号格式不正确")
    private String idCard;

    private String address;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20之间")
    private String password;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 从身份证号提取出生日期
     * @return 出生日期，如果身份证号无效则返回null
     */
    public LocalDate getBirthdayFromIdCard() {
        if (idCard == null || idCard.isEmpty()) {
            return null;
        }

        try {
            String birthdayStr;
            if (idCard.length() == 18) {
                birthdayStr = idCard.substring(6, 14);
            } else if (idCard.length() == 15) {
                birthdayStr = "19" + idCard.substring(6, 12);
            } else {
                return null;
            }

            String formattedDate = birthdayStr.substring(0, 4) + "-"
                    + birthdayStr.substring(4, 6) + "-"
                    + birthdayStr.substring(6, 8);

            return LocalDate.parse(formattedDate);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从身份证号识别性别
     * @return 1-男, 2-女, null-无法识别
     */
    public Integer getGenderFromIdCard() {
        if (idCard == null || idCard.isEmpty()) {
            return null;
        }

        try {
            char genderChar;
            if (idCard.length() == 18) {
                // 18位身份证：第17位为性别标识，奇数男，偶数女
                genderChar = idCard.charAt(16);
            } else if (idCard.length() == 15) {
                // 15位身份证：第15位为性别标识，奇数男，偶数女
                genderChar = idCard.charAt(14);
            } else {
                return null;
            }

            int genderNum = Character.getNumericValue(genderChar);
            // 奇数代表男性，偶数代表女性
            return (genderNum % 2 == 1) ? 1 : 2;
        } catch (Exception e) {
            return null;
        }
    }
}
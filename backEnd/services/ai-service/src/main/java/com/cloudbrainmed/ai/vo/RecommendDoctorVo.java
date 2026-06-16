package com.cloudbrainmed.ai.vo;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendDoctorVo {

    private String doctorId;        // 医生ID
    private String name;            // 医生姓名
    private String position;        // 职称/职位
    private String goodAt;          // 擅长领域
    private String introduction;    // 个人简介
    private String avatar;          // 头像地址
    private String departmentId;    // 科室ID
    private String departmentName;  // 科室名称（需要关联查询）
    private BigDecimal matchScore;  // 匹配分值（0-100）
    private String matchReason;     // 匹配原因简述
}
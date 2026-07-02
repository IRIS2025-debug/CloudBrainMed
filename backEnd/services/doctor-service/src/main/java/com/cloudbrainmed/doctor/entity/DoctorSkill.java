package com.cloudbrainmed.doctor.entity;

import lombok.Data;

/**
 * 医生技能表实体
 * 记录医生擅长处理的检查检验项目类型
 * 用于任务调度时的技能匹配
 */
@Data
public class DoctorSkill {
    private String skillId;
    private String doctorId;
    private String itemCode;
    private Integer priority;
    private Integer status;
}
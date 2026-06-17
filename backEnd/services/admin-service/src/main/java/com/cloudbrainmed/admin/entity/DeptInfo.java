package com.cloudbrainmed.admin.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("department")
public class DeptInfo {
    @TableId
    private String deptId;
    private String deptName;
    private Integer status;
    private LocalDateTime createTime;
}
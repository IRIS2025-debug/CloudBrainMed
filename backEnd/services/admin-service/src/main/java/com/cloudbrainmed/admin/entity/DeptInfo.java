package com.cloudbrainmed.admin.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("department")
public class DeptInfo {
    @TableId
    private String deptId;
    private String deptName;
    private String roomId;
    private Integer maxCapacity;
    private Integer freeCapacity;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createTime;
}

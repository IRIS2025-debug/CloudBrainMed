package com.cloudbrainmed.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@TableName("department")
public class Dept {
    @TableId(type = IdType.ASSIGN_ID)
    private String deptId;
    private String deptName;
    private String roomId;
    private Integer maxCapacity;
    private Integer freeCapacity;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime  createTime;
}
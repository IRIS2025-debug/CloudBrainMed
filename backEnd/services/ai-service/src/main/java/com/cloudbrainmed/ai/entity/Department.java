package com.cloudbrainmed.ai.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 科室表
 */
@Data
@TableName("department")
public class Department {
    @TableId
    private String deptId;
    private String deptName;
    private String roomId;
    private Integer maxCapacity;
    private Integer freeCapacity;
    private Integer status;
    private LocalDateTime createTime;
}

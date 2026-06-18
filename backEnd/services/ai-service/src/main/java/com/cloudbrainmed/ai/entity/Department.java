package com.cloudbrainmed.ai.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 科室表
 */
@Data
public class Department {

    /**
     * 科室ID
     */
    private String deptId;

    /**
     * 科室名称
     */
    private String deptName;

    /**
     * 房间ID
     */
    private String roomId;

    /**
     * 最大容纳人数
     */
    private Integer maxCapacity;

    /**
     * 空闲容纳人数
     */
    private Integer freeCapacity;

    /**
     * 状态：0=停用，1=启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
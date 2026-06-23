package com.cloudbrainmed.admin.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 药品 DTO
 */
@Data
public class MedicineDto {
    private String medicineId;
    private String name;
    private String spec;
    private String usage;
    private String indication;
    private String attention;
    private Integer stock;
    private BigDecimal price;

    // 以下字段用于前端展示，不存储在数据库中
    private Integer minStock;      // 预警线（前端配置，不存数据库）
    private Integer reorderQuantity; // 建议补货量（前端配置，不存数据库）
    private String status;         // 状态（前端计算）

}
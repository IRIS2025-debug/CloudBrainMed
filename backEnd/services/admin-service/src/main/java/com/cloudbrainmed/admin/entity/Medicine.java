package com.cloudbrainmed.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 药品表(对应 medicine)
 */
@Data
@TableName("medicine")
public class Medicine {
    private String medicineId;
    private String name;
    private String spec;
    private String usage;
    private String indication;
    private String attention;
    private Integer stock;
    private BigDecimal price;
    private LocalDateTime createTime;
}

package com.cloudbrainmed.doctor.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MedicineOptionDto {
    private String medicineId;
    private String name;
    private String spec;
    private String usage;
    private String indication;
    private String attention;
    private Integer stock;
    private BigDecimal price;
    private LocalDateTime createTime;

    public String getMedicineId() { return medicineId; }
    public void setMedicineId(String medicineId) { this.medicineId = medicineId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }
    public String getUsage() { return usage; }
    public void setUsage(String usage) { this.usage = usage; }
    public String getIndication() { return indication; }
    public void setIndication(String indication) { this.indication = indication; }
    public String getAttention() { return attention; }
    public void setAttention(String attention) { this.attention = attention; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}

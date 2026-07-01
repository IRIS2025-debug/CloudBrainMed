package com.cloudbrainmed.doctor.dto;

import java.math.BigDecimal;

/**
 * 开具处方请求 DTO
 */
public class PrescriptionCreateDto {
    private String registerId;
    private String medicineId;
    private String medicineName;
    private String spec;
    private String usage;
    private Integer num;
    private BigDecimal price;

    public String getRegisterId() { return registerId; }
    public void setRegisterId(String registerId) { this.registerId = registerId; }
    public String getMedicineId() { return medicineId; }
    public void setMedicineId(String medicineId) { this.medicineId = medicineId; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }
    public String getUsage() { return usage; }
    public void setUsage(String usage) { this.usage = usage; }
    public Integer getNum() { return num; }
    public void setNum(Integer num) { this.num = num; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}

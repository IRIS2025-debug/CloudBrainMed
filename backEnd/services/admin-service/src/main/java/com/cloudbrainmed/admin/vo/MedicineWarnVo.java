package com.cloudbrainmed.admin.vo;

/**
 * 药品预警 VO
 */
public class MedicineWarnVo {
    private String medicineId;
    private String name;
    private String spec;
    private Integer stock;
    private Integer minStock;          // 预警线（前端配置）
    private String status;             // LOW_STOCK, OUT_OF_STOCK
    private String warnType;           // STOCK_WARNING, REORDER_SUGGEST
    private Integer suggestedReorder;  // 建议补货数量

    public MedicineWarnVo() {}

    public MedicineWarnVo(String medicineId, String name, String spec, Integer stock,
                          Integer minStock, String status, String warnType, Integer suggestedReorder) {
        this.medicineId = medicineId;
        this.name = name;
        this.spec = spec;
        this.stock = stock;
        this.minStock = minStock;
        this.status = status;
        this.warnType = warnType;
        this.suggestedReorder = suggestedReorder;
    }

    // getters and setters
    public String getMedicineId() { return medicineId; }
    public void setMedicineId(String medicineId) { this.medicineId = medicineId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Integer getMinStock() { return minStock; }
    public void setMinStock(Integer minStock) { this.minStock = minStock; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getWarnType() { return warnType; }
    public void setWarnType(String warnType) { this.warnType = warnType; }

    public Integer getSuggestedReorder() { return suggestedReorder; }
    public void setSuggestedReorder(Integer suggestedReorder) { this.suggestedReorder = suggestedReorder; }
}
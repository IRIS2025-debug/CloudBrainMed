package com.cloudbrainmed.admin.service;

import com.cloudbrainmed.admin.dto.MedicineDto;
import com.cloudbrainmed.admin.entity.Medicine;
import com.cloudbrainmed.admin.vo.MedicineWarnVo;

import java.util.List;

public interface MedicineService {

    /**
     * 查询药品列表
     */
    List<MedicineDto> list(String keyword);

    /**
     * 根据ID查询
     */
    MedicineDto getById(String medicineId);

    /**
     * 新增药品
     */
    boolean add(MedicineDto dto);

    /**
     * 更新药品
     */
    boolean update(MedicineDto dto);

    /**
     * 删除药品
     */
    boolean delete(String medicineId);

    /**
     * 扣除库存
     */
    boolean deductStock(String medicineId, Integer quantity);

    /**
     * 增加库存（补货）
     */
    boolean addStock(String medicineId, Integer quantity);

    /**
     * 获取库存预警列表（需要前端配置预警线）
     */
    List<MedicineWarnVo> getWarnList(Integer minStockThreshold);

    /**
     * 获取智能补货建议
     */
    List<MedicineWarnVo> getReorderSuggestions(Integer minStockThreshold, Integer reorderQuantity);
}
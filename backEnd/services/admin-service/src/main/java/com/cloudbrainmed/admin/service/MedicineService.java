package com.cloudbrainmed.admin.service;

import com.cloudbrainmed.admin.dto.MedicineDto;
import com.cloudbrainmed.admin.vo.MedicineWarnVo;

import java.util.List;

public interface MedicineService {

    List<MedicineDto> list(String keyword);

    MedicineDto getById(String medicineId);

    boolean add(MedicineDto dto);

    boolean update(MedicineDto dto);

    boolean delete(String medicineId);

    boolean deductStock(String medicineId, Integer quantity);

    boolean addStock(String medicineId, Integer quantity);

    List<MedicineWarnVo> getWarnList(Integer minStockThreshold);

    List<MedicineWarnVo> getReorderSuggestions(Integer minStockThreshold, Integer reorderQuantity);

    boolean batchUpdate(List<MedicineDto> dtoList);

    /**
     * 更新全局预警线
     */
    boolean updateGlobalWarnThreshold(Integer threshold);

    /**
     * 获取全局预警线
     */
    Integer getGlobalWarnThreshold();
}
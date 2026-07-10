package com.cloudbrainmed.admin.service.impl;

import com.cloudbrainmed.admin.dto.MedicineDto;
import com.cloudbrainmed.admin.entity.Medicine;
import com.cloudbrainmed.admin.mapper.MedicineMapper;
import com.cloudbrainmed.admin.service.MedicineService;
import com.cloudbrainmed.admin.vo.MedicineWarnVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MedicineServiceImpl implements MedicineService {

    // 默认预警线
    private static final int DEFAULT_MIN_STOCK = 10;
    // 默认建议补货量
    private static final int DEFAULT_REORDER_QUANTITY = 50;

    private final MedicineMapper medicineMapper;

    public MedicineServiceImpl(MedicineMapper medicineMapper) {
        this.medicineMapper = medicineMapper;
    }

    @Override
    public List<MedicineDto> list(String keyword) {
        List<Medicine> medicines;
        if (keyword != null && !keyword.isEmpty()) {
            medicines = medicineMapper.selectByName(keyword);
        } else {
            medicines = medicineMapper.selectAll();
        }
        return convertToDtoList(medicines);
    }

    @Override
    public MedicineDto getById(String medicineId) {
        Medicine medicine = medicineMapper.selectById(medicineId);
        return convertToDto(medicine);
    }

    @Override
    @Transactional
    public boolean add(MedicineDto dto) {
        Medicine medicine = new Medicine();
        BeanUtils.copyProperties(dto, medicine);
        medicine.setMedicineId("MED" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        return medicineMapper.insert(medicine) > 0;
    }

    @Override
    @Transactional
    public boolean update(MedicineDto dto) {
        Medicine medicine = new Medicine();
        BeanUtils.copyProperties(dto, medicine);
        return medicineMapper.update(medicine) > 0;
    }

    @Override
    @Transactional
    public boolean delete(String medicineId) {
        return medicineMapper.delete(medicineId) > 0;
    }

    @Override
    @Transactional
    public boolean deductStock(String medicineId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return false;
        }
        return medicineMapper.deductStock(medicineId, quantity) > 0;
    }

    @Override
    @Transactional
    public boolean addStock(String medicineId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return false;
        }
        return medicineMapper.addStock(medicineId, quantity) > 0;
    }

    @Override
    public List<MedicineWarnVo> getWarnList(Integer minStockThreshold) {
        int threshold = minStockThreshold != null ? minStockThreshold : DEFAULT_MIN_STOCK;
        List<Medicine> allMedicines = medicineMapper.selectAll();
        List<MedicineWarnVo> warnList = new ArrayList<>();

        for (Medicine m : allMedicines) {
            if (m.getStock() <= threshold) {
                String status = m.getStock() == 0 ? "OUT_OF_STOCK" : "LOW_STOCK";
                warnList.add(new MedicineWarnVo(
                        m.getMedicineId(), m.getName(), m.getSpec(), m.getStock(),
                        threshold, status, "STOCK_WARNING", null
                ));
            }
        }
        return warnList;
    }

    @Override
    public List<MedicineWarnVo> getReorderSuggestions(Integer minStockThreshold, Integer reorderQuantity) {
        int threshold = minStockThreshold != null ? minStockThreshold : DEFAULT_MIN_STOCK;
        int reorderQty = reorderQuantity != null ? reorderQuantity : DEFAULT_REORDER_QUANTITY;

        List<Medicine> allMedicines = medicineMapper.selectAll();
        List<MedicineWarnVo> suggestions = new ArrayList<>();

        for (Medicine m : allMedicines) {
            if (m.getStock() <= threshold + reorderQty) {
                String status = m.getStock() == 0 ? "OUT_OF_STOCK" : "LOW_STOCK";
                // 建议补货量：如果库存为0，补货量为 reorderQty * 2，否则为 reorderQty
                int suggested = m.getStock() == 0 ? reorderQty * 2 : reorderQty;
                suggestions.add(new MedicineWarnVo(
                        m.getMedicineId(), m.getName(), m.getSpec(), m.getStock(),
                        threshold, status, "REORDER_SUGGEST", suggested
                ));
            }
        }
        return suggestions;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdate(List<MedicineDto> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return false;
        }

        List<Medicine> entities = new ArrayList<>();
        for (MedicineDto dto : dtoList) {
            Medicine entity = new Medicine();
            entity.setMedicineId(dto.getMedicineId());
            entity.setName(dto.getName());
            entity.setSpec(dto.getSpec());
            entity.setUsage(dto.getUsage());
            entity.setIndication(dto.getIndication());
            entity.setAttention(dto.getAttention());
            entity.setStock(dto.getStock());
            entity.setPrice(dto.getPrice());
            entities.add(entity);
        }

        // 使用批量更新（只更新非空字段）
        int result = medicineMapper.batchUpdateSelective(entities);
        return result == dtoList.size();
    }

    /**
     * 转换为 DTO
     */
    private MedicineDto convertToDto(Medicine medicine) {
        if (medicine == null) {
            return null;
        }
        MedicineDto dto = new MedicineDto();
        BeanUtils.copyProperties(medicine, dto);
        // 计算状态
        dto.setStatus(determineStatus(medicine.getStock(), DEFAULT_MIN_STOCK));
        dto.setMinStock(DEFAULT_MIN_STOCK);
        dto.setReorderQuantity(DEFAULT_REORDER_QUANTITY);
        return dto;
    }

    /**
     * 转换为 DTO 列表
     */
    private List<MedicineDto> convertToDtoList(List<Medicine> medicines) {
        List<MedicineDto> dtoList = new ArrayList<>();
        for (Medicine m : medicines) {
            dtoList.add(convertToDto(m));
        }
        return dtoList;
    }

    /**
     * 判断库存状态
     */
    private String determineStatus(Integer stock, Integer minStock) {
        if (stock == null || stock <= 0) {
            return "OUT_OF_STOCK";
        }
        if (minStock != null && stock <= minStock) {
            return "LOW_STOCK";
        }
        return "NORMAL";
    }
}
package com.cloudbrainmed.admin.controller;

import com.cloudbrainmed.admin.dto.MedicineDto;
import com.cloudbrainmed.admin.service.MedicineService;
import com.cloudbrainmed.admin.vo.MedicineWarnVo;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin-service/medicine")
public class MedicineManageController {

    private final MedicineService medicineService;

    public MedicineManageController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    /**
     * 获取药品列表
     */
    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam(required = false) String keyword) {
        List<MedicineDto> list = medicineService.list(keyword);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", list);
        return result;
    }

    /**
     * 获取药品详情
     */
    @GetMapping("/{medicineId}")
    public Map<String, Object> getById(@PathVariable String medicineId) {
        MedicineDto medicine = medicineService.getById(medicineId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", medicine);
        return result;
    }

    /**
     * 新增药品
     */
    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody MedicineDto dto) {
        boolean success = medicineService.add(dto);
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("message", success ? "添加成功" : "添加失败");
        return result;
    }

    /**
     * 更新药品
     */
    @PutMapping("/update")
    public Map<String, Object> update(@RequestBody MedicineDto dto) {
        boolean success = medicineService.update(dto);
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("message", success ? "更新成功" : "更新失败");
        return result;
    }

    /**
     * 删除药品
     */
    @DeleteMapping("/{medicineId}")
    public Map<String, Object> delete(@PathVariable String medicineId) {
        boolean success = medicineService.delete(medicineId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("message", success ? "删除成功" : "删除失败");
        return result;
    }

    /**
     * 扣除库存
     */
    @PostMapping("/deduct")
    public Map<String, Object> deductStock(@RequestBody Map<String, Object> params) {
        String medicineId = (String) params.get("medicineId");
        Integer quantity = (Integer) params.get("quantity");
        boolean success = medicineService.deductStock(medicineId, quantity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("message", success ? "扣库存成功" : "扣库存失败（库存不足）");
        return result;
    }

    /**
     * 增加库存（补货）
     */
    @PostMapping("/add-stock")
    public Map<String, Object> addStock(@RequestBody Map<String, Object> params) {
        String medicineId = (String) params.get("medicineId");
        Integer quantity = (Integer) params.get("quantity");
        boolean success = medicineService.addStock(medicineId, quantity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("message", success ? "补货成功" : "补货失败");
        return result;
    }

    /**
     * 获取库存预警列表
     */
    @GetMapping("/warnings")
    public Map<String, Object> getWarnings(@RequestParam(required = false) Integer minStock) {
        List<MedicineWarnVo> warnings = medicineService.getWarnList(minStock);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", warnings);
        return result;
    }

    /**
     * 获取智能补货建议
     */
    @GetMapping("/reorder-suggestions")
    public Map<String, Object> getReorderSuggestions(
            @RequestParam(required = false) Integer minStock,
            @RequestParam(required = false) Integer reorderQuantity) {
        List<MedicineWarnVo> suggestions = medicineService.getReorderSuggestions(minStock, reorderQuantity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", suggestions);
        return result;
    }

    /**
     * 批量更新药品
     */
    @PutMapping("/batch-update")
    public Map<String, Object> batchUpdate(@RequestBody List<MedicineDto> dtoList) {
        boolean success = medicineService.batchUpdate(dtoList);
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("message", success ? "批量更新成功" : "批量更新失败");
        return result;
    }
}
package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.doctor.entity.ConsultRecord;
import com.cloudbrainmed.doctor.mapper.ConsultMapper;
import com.cloudbrainmed.doctor.service.ConsultService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ConsultServiceImpl implements ConsultService {

    private final ConsultMapper consultMapper;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public ConsultServiceImpl(ConsultMapper consultMapper) {
        this.consultMapper = consultMapper;
    }

    @Override
    public List<ConsultRecord> getList(String doctorId, String consultStatus, String date, int page, int limit) {
        int offset = (page - 1) * limit;
        return consultMapper.findList(doctorId, consultStatus, date, offset, limit);
    }

    @Override
    public ConsultRecord getDetail(String doctorId, String registerId) {
        ConsultRecord r = consultMapper.findDetail(registerId);
        if (r == null) throw new BusinessException("就诊记录不存在");
        if (!doctorId.equals(r.getDoctorId())) {
            throw new BusinessException("无权查看该接诊记录");
        }
        return r;
    }

    @Override
    public void saveDraft(String registerId, String recordDesc) {
        ConsultRecord detail = consultMapper.findDetail(registerId);
        if (detail == null) throw new BusinessException("就诊记录不存在");

        String recordId = consultMapper.findRecordId(registerId);
        if (recordId == null) {
            // 新建病历
            ConsultRecord r = new ConsultRecord();
            r.setRecordId("REC" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            r.setPatientId(detail.getPatientId());
            r.setDoctorId(detail.getDoctorId());
            r.setRegisterId(registerId);
            r.setDoctorName(detail.getDoctorName());
            r.setPatientName(detail.getPatientName() != null ? detail.getPatientName() : detail.getName());
            r.setVisitAge(detail.getPatientAge());
            r.setDescription(recordDesc);
            r.setVisitDate(detail.getVisitDate() != null ? detail.getVisitDate() : LocalDate.now());
            consultMapper.insertRecord(r);
        } else {
            consultMapper.updateRecordDesc(registerId, recordDesc);
        }
        consultMapper.markInProgress(registerId);
    }

    @Override
    public void confirmRecord(String registerId, String recordDesc) {
        saveDraft(registerId, recordDesc);
        consultMapper.markRecordConfirmed(registerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createExamOrder(String registerId, String checkItemList, String urgencyLevel) {
        ConsultRecord detail = consultMapper.findDetail(registerId);
        if (detail == null) throw new BusinessException("就诊记录不存在");

        // ----- 解析前端传来的检查项目 JSON -----
        if (checkItemList == null || checkItemList.trim().isEmpty()) {
            throw new BusinessException("检查项目列表为空");
        }
        List<Map<String, String>> items;
        try {
            items = objectMapper.readValue(checkItemList,
                    new TypeReference<List<Map<String, String>>>() {});
        } catch (JsonProcessingException e) {
            throw new BusinessException("检查项目格式错误: " + e.getMessage(), e);
        }
        if (items.isEmpty()) {
            throw new BusinessException("检查项目列表为空");
        }

        // ----- 批量查字典，避免 N+1 -----
        List<String> itemNames = items.stream()
                .map(i -> i.get("itemName"))
                .filter(n -> n != null && !n.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        Map<String, Map<String, Object>> dictMap = new HashMap<>();
        if (!itemNames.isEmpty()) {
            List<Map<String, Object>> dictRows = consultMapper.findMedicalItemsByNames(itemNames);
            if (dictRows != null) {
                for (Map<String, Object> row : dictRows) {
                    dictMap.put((String) row.get("item_name"), row);
                }
            }
        }

        // ----- 写入主表 -----
        String orderId = "CHK" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        consultMapper.insertCheckReport(orderId, detail.getPatientId(), registerId,
                detail.getDoctorId(), checkItemList, urgencyLevel);

        // ----- 逐项写入子表 -----
        for (Map<String, String> item : items) {
            String itemName = item.get("itemName");
            if (itemName == null || itemName.trim().isEmpty()) continue;
            itemName = itemName.trim();
            Map<String, Object> dict = dictMap.get(itemName);

            String orderItemId = "CHKI" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
            if (dict == null) {
                consultMapper.insertOrderItem(orderItemId, orderId, null, null,
                        itemName, "EXAM", item.get("dept"), urgencyLevel, BigDecimal.ZERO);
            } else {
                consultMapper.insertOrderItem(orderItemId, orderId,
                        (String) dict.get("item_id"),
                        (String) dict.get("item_code"),
                        (String) dict.get("item_name"),
                        (String) dict.get("item_category"),
                        item.get("dept") != null ? item.get("dept") : (String) dict.get("dept_id"),
                        urgencyLevel,
                        toBigDecimal(dict.get("price")));
            }
        }
    }

    private static BigDecimal toBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal bd) return bd;
        if (val instanceof Number num) return BigDecimal.valueOf(num.doubleValue());
        try {
            return new BigDecimal(val.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public void completeConsult(String registerId, String doctorId) {
        ConsultRecord detail = consultMapper.findDetail(registerId);
        if (detail == null) throw new BusinessException("就诊记录不存在");
        if (!doctorId.equals(detail.getDoctorId())) {
            throw new BusinessException("无权操作该接诊记录");
        }
        consultMapper.completeConsult(registerId);
    }
}
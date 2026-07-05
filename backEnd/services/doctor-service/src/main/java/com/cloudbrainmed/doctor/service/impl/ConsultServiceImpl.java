package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.api.feign.PaymentFeignClient;
import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.doctor.entity.ConsultRecord;
import com.cloudbrainmed.doctor.mapper.ConsultMapper;
import com.cloudbrainmed.doctor.service.ConsultService;
import com.cloudbrainmed.payment.dto.UnifiedPayDto;
import com.cloudbrainmed.payment.vo.PayResultVo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ConsultServiceImpl implements ConsultService {

    private final ConsultMapper consultMapper;
    private final PaymentFeignClient paymentFeignClient;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public ConsultServiceImpl(ConsultMapper consultMapper,
                              PaymentFeignClient paymentFeignClient) {
        this.consultMapper = consultMapper;
        this.paymentFeignClient = paymentFeignClient;
    }

    @Override
    public List<ConsultRecord> getList(
            String doctorId,
            String consultStatus,
            String date,
            boolean reportReturnedOnly,
            int page,
            int limit) {
        int offset = (page - 1) * limit;
        return consultMapper.findList(
                doctorId, consultStatus, date, reportReturnedOnly, offset, limit);
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
    public void saveDraft(String doctorId, String registerId, String recordDesc) {
        ConsultRecord detail = consultMapper.findDetail(registerId);
        if (detail == null) throw new BusinessException("就诊记录不存在");
        ensureDoctorCanEdit(doctorId, detail);
        if ("COMPLETED".equals(detail.getConsultStatus())) {
            throw new BusinessException("接诊已完成，不能继续修改病历");
        }

        String recordId = consultMapper.findRecordId(registerId);
        if (recordId == null) {
            // 新建病历
            ConsultRecord r = new ConsultRecord();
            r.setRecordId("REC" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            r.setPatientId(detail.getPatientId());
            r.setDoctorId(detail.getDoctorId());
            r.setRegisterId(registerId);
            r.setDoctorName(resolveDoctorName(detail));
            r.setPatientName(detail.getPatientName() != null ? detail.getPatientName() : detail.getName());
            r.setVisitAge(detail.getPatientAge());
            r.setDescription(recordDesc);
            r.setVisitDate(detail.getVisitDate() != null ? detail.getVisitDate() : LocalDate.now());
            r.setPayStatus(detail.getPayStatus());
            r.setCreateTime(LocalDateTime.now());
            consultMapper.insertRecord(r);
        } else {
            consultMapper.updateRecordDesc(registerId, recordDesc);
        }
        consultMapper.markInProgress(registerId);
    }

    @Override
    public void confirmRecord(String doctorId, String registerId, String recordDesc) {
        saveDraft(doctorId, registerId, recordDesc);
        consultMapper.markRecordConfirmed(registerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createExamOrder(String doctorId, String registerId, String checkItemList, String urgencyLevel) {
        ConsultRecord detail = consultMapper.findDetail(registerId);
        if (detail == null) throw new BusinessException("就诊记录不存在");
        ensureDoctorCanEdit(doctorId, detail);
        if ("COMPLETED".equals(detail.getConsultStatus())) {
            throw new BusinessException("接诊已完成，不能继续开具检查检验申请");
        }

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
        for (String itemName : itemNames) {
            if (!dictMap.containsKey(itemName)) {
                throw new BusinessException("medical item not found: " + itemName);
            }
        }

        // ----- 写入主表 -----
        String orderId = "CHK" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        // 构建临床摘要：提取症状+初步诊断+申请目的，便于检验医生快速了解病情
        String clinicalSummary = buildClinicalSummary(detail);
        consultMapper.insertCheckReport(orderId, detail.getPatientId(), registerId,
                detail.getDoctorId(), clinicalSummary, urgencyLevel);

        // ----- 逐项写入子表 -----
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Map<String, String> item : items) {
            String itemName = item.get("itemName");
            if (itemName == null || itemName.trim().isEmpty()) continue;
            itemName = itemName.trim();
            Map<String, Object> dict = dictMap.get(itemName);

            String orderItemId = "CHKI" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
            BigDecimal price = toBigDecimal(dict.get("price"));
            consultMapper.insertOrderItem(orderItemId, orderId,
                    (String) dict.get("item_id"),
                    (String) dict.get("item_code"),
                    (String) dict.get("item_name"),
                    (String) dict.get("item_category"),
                    item.get("dept") != null ? item.get("dept") : (String) dict.get("dept_id"),
                    urgencyLevel,
                    price);
            totalAmount = totalAmount.add(price);
        }
        createPayOrder(orderId, detail, totalAmount);
    }

    /**
     * 构建临床摘要
     * 格式："症状：xxx\n初步诊断：xxx\n申请目的：为进一步明确诊断，建议行xxx检查"
     * 从病历中提取症状(主诉)和初步诊断(诊断意见)，结合申请项目构建申请目的
     */
    private String buildClinicalSummary(ConsultRecord detail) {
        StringBuilder sb = new StringBuilder();
        String description = detail.getDescription();

        // 1. 症状：优先使用挂号主诉，其次从病历 description 中提取"主诉"段落
        String symptom = detail.getChiefComplaint();
        if (symptom == null || symptom.isBlank()) {
            symptom = extractSection(description, "主诉");
        }
        if (symptom != null && !symptom.isBlank()) {
            sb.append("症状：").append(symptom.trim());
        }

        // 2. 初步诊断：从病历 description 中提取"诊断意见"段落
        String diagnosis = extractSection(description, "诊断意见");
        if (diagnosis != null && !diagnosis.isBlank()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("初步诊断：").append(diagnosis.trim());
        }

        // 3. 申请目的：如果没有任何病历信息，至少给出主诉作为临床摘要
        if (sb.length() == 0 && description != null && !description.isBlank()) {
            sb.append(description.trim());
        }

        return sb.toString();
    }

    /**
     * 从病历文本中提取指定段落的内容
     * 匹配格式："段落名：\n内容" 或 "段落名：内容"
     * 自动在下一个段落标题处截断
     */
    private String extractSection(String text, String sectionName) {
        if (text == null || text.isBlank()) return null;
        String pattern = sectionName + "：";
        int idx = text.indexOf(pattern);
        if (idx < 0) {
            // 尝试带换行的格式："段落名：\n"
            pattern = sectionName + "：\n";
            idx = text.indexOf(pattern);
            if (idx < 0) return null;
        }
        int start = idx + pattern.length();
        // 寻找下一个段落标题的位置
        int end = text.length();
        String[] headings = {"主诉", "现病史", "既往史", "体格检查", "辅助检查", "诊断意见", "处理计划"};
        for (String h : headings) {
            if (h.equals(sectionName)) continue;
            int hIdx = text.indexOf(h + "：", start);
            if (hIdx >= 0 && hIdx < end) {
                end = hIdx;
            }
        }
        String content = text.substring(start, end).trim();
        return content.isEmpty() ? null : content;
    }

    private void createPayOrder(
            String orderId, ConsultRecord detail, BigDecimal totalAmount) {
        UnifiedPayDto dto = new UnifiedPayDto();
        dto.setPatientId(detail.getPatientId());
        dto.setPatientName(patientName(detail));
        dto.setOrderType("MEDICAL");
        dto.setBusinessId(orderId);
        dto.setDescription("医技检查检验费");
        dto.setAmount(totalAmount == null ? BigDecimal.ZERO : totalAmount);
        Result<PayResultVo> result = paymentFeignClient.createPayOrder(dto);
        if (result == null || result.getCode() == null || result.getCode() != 200) {
            throw new BusinessException("create medical pay order failed");
        }
    }

    private String patientName(ConsultRecord detail) {
        if (detail.getPatientName() != null && !detail.getPatientName().isBlank()) {
            return detail.getPatientName();
        }
        return detail.getName();
    }

    private String resolveDoctorName(ConsultRecord detail) {
        if (detail.getDoctorName() != null && !detail.getDoctorName().isBlank()) {
            return detail.getDoctorName();
        }
        return consultMapper.findDoctorName(detail.getDoctorId());
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
    public void completeConsult(String doctorId, String registerId) {
        ConsultRecord detail = consultMapper.findDetail(registerId);
        if (detail == null) throw new BusinessException("就诊记录不存在");
        ensureDoctorCanEdit(doctorId, detail);
        if (!"RECORD_CONFIRMED".equals(detail.getConsultStatus())) {
            throw new BusinessException("请先确认病历后再完成接诊");
        }
        consultMapper.completeConsult(registerId);
    }

    private void ensureDoctorCanEdit(String doctorId, ConsultRecord detail) {
        if (!doctorId.equals(detail.getDoctorId())) {
            throw new BusinessException("无权操作该接诊记录");
        }
    }
}
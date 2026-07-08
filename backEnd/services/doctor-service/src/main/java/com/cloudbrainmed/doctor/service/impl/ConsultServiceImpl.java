package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.api.feign.PaymentFeignClient;
import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.doctor.entity.ConsultRecord;
import com.cloudbrainmed.doctor.mapper.ConsultMapper;
import com.cloudbrainmed.doctor.mapper.MedicalOrderMapper;
import com.cloudbrainmed.doctor.service.ConsultService;
import com.cloudbrainmed.payment.dto.UnifiedPayDto;
import com.cloudbrainmed.payment.vo.PayResultVo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

@Slf4j
@Service
public class ConsultServiceImpl implements ConsultService {

    private final ConsultMapper consultMapper;
    private final PaymentFeignClient paymentFeignClient;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final MedicalOrderMapper medicalOrderMapper;
    private final boolean devAutoQueueAfterConfirm;

    public ConsultServiceImpl(ConsultMapper consultMapper,
                              PaymentFeignClient paymentFeignClient,
                              MedicalOrderMapper medicalOrderMapper,
                              @Value("${cloudbrainmed.doctor.medical-order.dev-auto-queue-after-confirm:false}")
                              boolean devAutoQueueAfterConfirm) {
        this.consultMapper = consultMapper;
        this.paymentFeignClient = paymentFeignClient;
        this.medicalOrderMapper = medicalOrderMapper;
        this.devAutoQueueAfterConfirm = devAutoQueueAfterConfirm;
    }

    @Override
    public List<ConsultRecord> getList(String doctorId, String consultStatus,
                                       String date, boolean reportReturnedOnly,
                                       int page, int limit) {
        int offset = (page - 1) * limit;
        List<ConsultRecord> records = consultMapper.findList(
                doctorId, consultStatus, date, reportReturnedOnly, offset, limit);
        records.forEach(ConsultAccessGuard::applyListEntryState);
        return records;
    }

    @Override
    public ConsultRecord getDetail(String doctorId, String registerId) {
        return ConsultAccessGuard.requireDetailAccess(
                consultMapper, registerId, doctorId);
    }

    @Override
    public void saveDraft(String doctorId, String registerId,
                          String recordDesc) {
        ConsultRecord detail = ConsultAccessGuard.requireActionAccess(
                consultMapper, registerId, doctorId);
        if ("COMPLETED".equals(detail.getConsultStatus())) {
            throw new BusinessException("接诊已完成，不能继续修改病历");
        }

        String recordId = consultMapper.findRecordId(registerId);
        if (recordId == null) {
            ConsultRecord record = new ConsultRecord();
            record.setRecordId("REC" + UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 16));
            record.setPatientId(detail.getPatientId());
            record.setDoctorId(detail.getDoctorId());
            record.setRegisterId(registerId);
            record.setDoctorName(resolveDoctorName(detail));
            record.setPatientName(detail.getPatientName() != null
                    ? detail.getPatientName() : detail.getName());
            record.setVisitAge(detail.getPatientAge());
            record.setDescription(recordDesc);
            record.setVisitDate(detail.getVisitDate() != null
                    ? detail.getVisitDate() : LocalDate.now());
            record.setPayStatus(detail.getPayStatus());
            record.setCreateTime(LocalDateTime.now());
            consultMapper.insertRecord(record);
        } else {
            consultMapper.updateRecordDesc(registerId, recordDesc);
        }
        consultMapper.markInProgress(registerId);
    }

    @Override
    public void confirmRecord(String doctorId, String registerId,
                              String recordDesc) {
        saveDraft(doctorId, registerId, recordDesc);
        consultMapper.markRecordConfirmed(registerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createExamOrder(String doctorId, String registerId,
                                String checkItemList, String urgencyLevel) {
        ConsultRecord detail = ConsultAccessGuard.requireActionAccess(
                consultMapper, registerId, doctorId);
        if ("COMPLETED".equals(detail.getConsultStatus())) {
            throw new BusinessException("接诊已完成，不能继续开具检查检验申请");
        }

        if (checkItemList == null || checkItemList.trim().isEmpty()) {
            throw new BusinessException("检查项目列表为空");
        }
        List<Map<String, String>> items;
        try {
            items = objectMapper.readValue(checkItemList,
                    new TypeReference<List<Map<String, String>>>() {
                    });
        } catch (JsonProcessingException e) {
            throw new BusinessException("检查项目格式错误" + e.getMessage(), e);
        }
        if (items.isEmpty()) {
            throw new BusinessException("检查项目列表为空");
        }

        List<String> itemNames = items.stream()
                .map(item -> item.get("itemName"))
                .filter(name -> name != null && !name.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        Map<String, Map<String, Object>> dictMap = new HashMap<>();
        if (!itemNames.isEmpty()) {
            List<Map<String, Object>> dictRows =
                    consultMapper.findMedicalItemsByNames(itemNames);
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

        String orderId = "CHK" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 16);
        consultMapper.insertCheckReport(orderId, detail.getPatientId(),
                registerId, detail.getDoctorId(), checkItemList, urgencyLevel);

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Map<String, String> item : items) {
            String itemName = item.get("itemName");
            if (itemName == null || itemName.trim().isEmpty()) {
                continue;
            }
            Map<String, Object> dict = dictMap.get(itemName.trim());

            String orderItemId = "CHKI" + UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 14);
            BigDecimal price = toBigDecimal(dict.get("price"));
            consultMapper.insertOrderItem(orderItemId, orderId,
                    (String) dict.get("item_id"),
                    (String) dict.get("item_code"),
                    (String) dict.get("item_name"),
                    (String) dict.get("item_category"),
                    item.get("dept") != null ? item.get("dept")
                            : (String) dict.get("dept_id"),
                    urgencyLevel,
                    price);
            totalAmount = totalAmount.add(price);
        }
        try {
            createPayOrder(orderId, detail, totalAmount);
        } catch (RuntimeException e) {
            if (!devAutoQueueAfterConfirm) {
                throw e;
            }
            log.warn("Medical order {} payment creation failed; dev flow will queue it anyway",
                    orderId, e);
        }
        if (devAutoQueueAfterConfirm) {
            medicalOrderMapper.updatePayStatus(orderId);
            medicalOrderMapper.enqueueOrder(orderId);
            medicalOrderMapper.enqueueOrderItems(orderId);
        }
    }

    private void createPayOrder(String orderId, ConsultRecord detail,
                                BigDecimal totalAmount) {
        UnifiedPayDto dto = new UnifiedPayDto();
        dto.setPatientId(detail.getPatientId());
        dto.setPatientName(patientName(detail));
        dto.setOrderType("MEDICAL");
        dto.setBusinessId(orderId);
        dto.setDescription("医技检查检验费");
        dto.setAmount(totalAmount == null ? BigDecimal.ZERO : totalAmount);
        Result<PayResultVo> result = paymentFeignClient.createPayOrder(dto);
        if (result == null || result.getCode() == null
                || result.getCode() != 200) {
            throw new BusinessException("create medical pay order failed");
        }
    }

    private String patientName(ConsultRecord detail) {
        if (detail.getPatientName() != null
                && !detail.getPatientName().isBlank()) {
            return detail.getPatientName();
        }
        return detail.getName();
    }

    private String resolveDoctorName(ConsultRecord detail) {
        if (detail.getDoctorName() != null
                && !detail.getDoctorName().isBlank()) {
            return detail.getDoctorName();
        }
        return consultMapper.findDoctorName(detail.getDoctorId());
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public void completeConsult(String doctorId, String registerId) {
        ConsultRecord detail = ConsultAccessGuard.requireActionAccess(
                consultMapper, registerId, doctorId);
        if (!"RECORD_CONFIRMED".equals(detail.getConsultStatus())) {
            throw new BusinessException("请先确认病历后再完成接诊");
        }
        if (consultMapper.countPendingMedicalOrderItems(registerId) > 0) {
            throw new BusinessException("检查检验报告未全部回传，不能完成接诊");
        }
        consultMapper.completeConsult(registerId);
    }
}

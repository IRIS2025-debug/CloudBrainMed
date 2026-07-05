package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.api.feign.PaymentFeignClient;
import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.doctor.dto.PrescriptionCreateDto;
import com.cloudbrainmed.doctor.entity.Prescription;
import com.cloudbrainmed.doctor.mapper.PrescriptionMapper;
import com.cloudbrainmed.doctor.service.PrescriptionService;
import com.cloudbrainmed.payment.dto.UnifiedPayDto;
import com.cloudbrainmed.payment.vo.PayResultVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionMapper mapper;
    private final PaymentFeignClient paymentFeignClient;

    public PrescriptionServiceImpl(PrescriptionMapper mapper,
                                   PaymentFeignClient paymentFeignClient) {
        this.mapper = mapper;
        this.paymentFeignClient = paymentFeignClient;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Prescription create(PrescriptionCreateDto dto, String doctorId, String doctorName,
                               String patientId, String patientName) {
        Prescription p = new Prescription();
        p.setPrescriptionId("PRE" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase());
        p.setRegisterId(dto.getRegisterId());
        p.setPatientId(patientId);
        p.setDoctorId(doctorId);
        p.setDoctorName(doctorName);
        p.setPatientName(patientName);
        p.setMedicineId(dto.getMedicineId());
        p.setMedicineName(dto.getMedicineName());
        p.setSpec(dto.getSpec());
        p.setUsage(dto.getUsage());
        p.setNum(dto.getNum());
        p.setPrice(dto.getPrice());
        p.setPrescriptionDate(LocalDate.now());
        p.setPayStatus("WAITING");
        p.setCreateTime(LocalDateTime.now());
        mapper.insert(p);
        createPayOrder(p);
        return p;
    }

    private void createPayOrder(Prescription prescription) {
        UnifiedPayDto dto = new UnifiedPayDto();
        dto.setPatientId(prescription.getPatientId());
        dto.setPatientName(prescription.getPatientName());
        dto.setOrderType("PRESCRIPTION");
        dto.setBusinessId(prescription.getPrescriptionId());
        dto.setDescription("澶勬柟鑽搧璐圭敤");
        BigDecimal unitPrice = prescription.getPrice() == null
                ? BigDecimal.ZERO : prescription.getPrice();
        Integer num = prescription.getNum();
        dto.setAmount(unitPrice.multiply(BigDecimal.valueOf(num == null ? 1 : num)));
        Result<PayResultVo> result = paymentFeignClient.createPayOrder(dto);
        if (result == null || result.getCode() == null || result.getCode() != 200) {
            throw new BusinessException("create prescription pay order failed");
        }
    }

    @Override
    public List<Prescription> getByRegisterId(String registerId) {
        return mapper.selectByRegisterId(registerId);
    }

    @Override
    public Prescription getById(String prescriptionId) {
        return mapper.selectById(prescriptionId);
    }
}

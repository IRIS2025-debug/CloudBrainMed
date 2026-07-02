package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.api.feign.PaymentFeignClient;
import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.doctor.dto.PrescriptionCreateDto;
import com.cloudbrainmed.doctor.entity.Prescription;
import com.cloudbrainmed.doctor.mapper.PrescriptionMapper;
import com.cloudbrainmed.payment.dto.UnifiedPayDto;
import com.cloudbrainmed.payment.vo.PayResultVo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrescriptionServiceImplTest {

    private final PrescriptionMapper mapper = mock(PrescriptionMapper.class);
    private final PaymentFeignClient paymentFeignClient =
            mock(PaymentFeignClient.class);
    private final PrescriptionServiceImpl service =
            new PrescriptionServiceImpl(mapper, paymentFeignClient);

    @Test
    void createCreatesPrescriptionPayOrder() {
        when(paymentFeignClient.createPayOrder(any(UnifiedPayDto.class)))
                .thenReturn(Result.ok(new PayResultVo()));

        Prescription prescription = service.create(
                dto(), "D001", "Doctor", "P001", "Patient");

        ArgumentCaptor<Prescription> prescriptionCaptor =
                ArgumentCaptor.forClass(Prescription.class);
        ArgumentCaptor<UnifiedPayDto> payCaptor =
                ArgumentCaptor.forClass(UnifiedPayDto.class);
        verify(mapper).insert(prescriptionCaptor.capture());
        verify(paymentFeignClient).createPayOrder(payCaptor.capture());

        assertThat(prescription.getPayStatus()).isEqualTo("WAITING");
        assertThat(payCaptor.getValue().getOrderType())
                .isEqualTo("PRESCRIPTION");
        assertThat(payCaptor.getValue().getBusinessId())
                .isEqualTo(prescriptionCaptor.getValue().getPrescriptionId());
        assertThat(payCaptor.getValue().getPatientId()).isEqualTo("P001");
        assertThat(payCaptor.getValue().getPatientName()).isEqualTo("Patient");
        assertThat(payCaptor.getValue().getAmount())
                .isEqualByComparingTo("65.00");
    }

    @Test
    void createRollsBackWhenPayOrderCreationFails() {
        when(paymentFeignClient.createPayOrder(any(UnifiedPayDto.class)))
                .thenReturn(Result.error("pay failed"));

        assertThatThrownBy(() -> service.create(
                dto(), "D001", "Doctor", "P001", "Patient"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pay");
    }

    private PrescriptionCreateDto dto() {
        PrescriptionCreateDto dto = new PrescriptionCreateDto();
        dto.setRegisterId("REG001");
        dto.setMedicineId("MED001");
        dto.setMedicineName("Aspirin");
        dto.setSpec("100mg");
        dto.setUsage("once daily");
        dto.setNum(2);
        dto.setPrice(new BigDecimal("32.50"));
        return dto;
    }
}

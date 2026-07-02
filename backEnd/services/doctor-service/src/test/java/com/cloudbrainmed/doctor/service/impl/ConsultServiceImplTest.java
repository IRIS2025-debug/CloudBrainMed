package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.api.feign.PaymentFeignClient;
import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.doctor.entity.ConsultRecord;
import com.cloudbrainmed.doctor.mapper.ConsultMapper;
import com.cloudbrainmed.payment.dto.UnifiedPayDto;
import com.cloudbrainmed.payment.vo.PayResultVo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultServiceImplTest {

    private final ConsultMapper mapper = mock(ConsultMapper.class);
    private final PaymentFeignClient paymentFeignClient = mock(PaymentFeignClient.class);
    private final ConsultServiceImpl service = new ConsultServiceImpl(mapper, paymentFeignClient);

    @Test
    void saveDraftRejectsAnotherDoctor() {
        ConsultRecord record = consult("D001", "PENDING");
        when(mapper.findDetail("R001")).thenReturn(record);

        assertThrows(BusinessException.class,
                () -> service.saveDraft("D002", "R001", "record"));
    }

    @Test
    void saveDraftRejectsCompletedConsult() {
        ConsultRecord record = consult("D001", "COMPLETED");
        when(mapper.findDetail("R001")).thenReturn(record);

        assertThrows(BusinessException.class,
                () -> service.saveDraft("D001", "R001", "record"));
    }

    @Test
    void completeRequiresConfirmedRecord() {
        ConsultRecord record = consult("D001", "IN_PROGRESS");
        when(mapper.findDetail("R001")).thenReturn(record);

        assertThrows(BusinessException.class,
                () -> service.completeConsult("D001", "R001"));
    }

    @Test
    void createExamOrderRejectsAnotherDoctor() {
        ConsultRecord record = consult("D001", "IN_PROGRESS");
        when(mapper.findDetail("R001")).thenReturn(record);

        assertThrows(BusinessException.class,
                () -> service.createExamOrder("D002", "R001", "[{\"itemName\":\"CT\"}]", "NORMAL"));
        verify(mapper, never()).insertCheckReport(any(), any(), eq("R001"), any(), any(), any());
    }

    @Test
    void createExamOrderRejectsCompletedConsult() {
        ConsultRecord record = consult("D001", "COMPLETED");
        when(mapper.findDetail("R001")).thenReturn(record);

        assertThrows(BusinessException.class,
                () -> service.createExamOrder("D001", "R001", "[{\"itemName\":\"CT\"}]", "NORMAL"));
        verify(mapper, never()).insertCheckReport(any(), any(), eq("R001"), any(), any(), any());
    }

    @Test
    void createExamOrderCreatesMedicalPayOrder() {
        ConsultRecord record = consult("D001", "IN_PROGRESS");
        record.setPatientId("P001");
        record.setPatientName("Alice");
        when(mapper.findDetail("R001")).thenReturn(record);
        when(mapper.findMedicalItemsByNames(List.of("CT")))
                .thenReturn(List.of(Map.of(
                        "item_id", "ITEM001",
                        "item_code", "CRANIAL_CT_PLAIN",
                        "item_name", "CT",
                        "item_category", "EXAM",
                        "dept_id", "DEPT001",
                        "price", new BigDecimal("280.00"))));
        when(paymentFeignClient.createPayOrder(any(UnifiedPayDto.class)))
                .thenReturn(Result.ok(new PayResultVo()));

        service.createExamOrder("D001", "R001", "[{\"itemName\":\"CT\"}]", "NORMAL");

        ArgumentCaptor<UnifiedPayDto> payCaptor =
                ArgumentCaptor.forClass(UnifiedPayDto.class);
        verify(paymentFeignClient).createPayOrder(payCaptor.capture());

        UnifiedPayDto dto = payCaptor.getValue();
        assertThat(dto.getPatientId()).isEqualTo("P001");
        assertThat(dto.getPatientName()).isEqualTo("Alice");
        assertThat(dto.getOrderType()).isEqualTo("MEDICAL");
        assertThat(dto.getBusinessId()).startsWith("CHK");
        assertThat(dto.getAmount()).isEqualByComparingTo("280.00");
    }

    @Test
    void createExamOrderRejectsUnknownMedicalItem() {
        ConsultRecord record = consult("D001", "IN_PROGRESS");
        when(mapper.findDetail("R001")).thenReturn(record);
        when(mapper.findMedicalItemsByNames(List.of("Unknown CT")))
                .thenReturn(List.of());

        assertThrows(BusinessException.class,
                () -> service.createExamOrder("D001", "R001",
                        "[{\"itemName\":\"Unknown CT\"}]", "NORMAL"));

        verify(mapper, never()).insertOrderItem(any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(paymentFeignClient, never()).createPayOrder(any(UnifiedPayDto.class));
    }

    private ConsultRecord consult(String doctorId, String status) {
        ConsultRecord record = new ConsultRecord();
        record.setRegisterId("R001");
        record.setDoctorId(doctorId);
        record.setConsultStatus(status);
        return record;
    }
}

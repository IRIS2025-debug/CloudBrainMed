package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.api.feign.PaymentFeignClient;
import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.doctor.entity.ConsultRecord;
import com.cloudbrainmed.doctor.mapper.ConsultMapper;
import com.cloudbrainmed.doctor.mapper.MedicalOrderMapper;
import com.cloudbrainmed.payment.dto.UnifiedPayDto;
import com.cloudbrainmed.payment.vo.PayResultVo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
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
    private final MedicalOrderMapper medicalOrderMapper = mock(MedicalOrderMapper.class);
    private final ConsultServiceImpl service =
            new ConsultServiceImpl(mapper, paymentFeignClient, medicalOrderMapper, false);

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
    void saveDraftRejectsExpiredVisitDate() {
        ConsultRecord record = consult("D001", "PENDING");
        record.setVisitDate(LocalDate.now().minusDays(1));
        when(mapper.findDetail("R001")).thenReturn(record);

        assertThrows(BusinessException.class,
                () -> service.saveDraft("D001", "R001", "record"));
    }

    @Test
    void saveDraftRejectsFutureVisitDate() {
        ConsultRecord record = consult("D001", "PENDING");
        record.setVisitDate(LocalDate.now().plusDays(1));
        when(mapper.findDetail("R001")).thenReturn(record);

        assertThrows(BusinessException.class,
                () -> service.saveDraft("D001", "R001", "record"));
    }

    @Test
    void saveDraftCreatesRecordWithDoctorSnapshotAndCreateTime() {
        ConsultRecord record = consult("D001", "PENDING");
        record.setPatientId("P001");
        record.setName("Alice");
        record.setPatientAge(35);
        record.setVisitDate(LocalDate.now());
        record.setPayStatus("PAID");
        when(mapper.findDetail("R001")).thenReturn(record);
        when(mapper.findRecordId("R001")).thenReturn(null);
        when(mapper.findDoctorName("D001")).thenReturn("Dr. Li");

        service.saveDraft("D001", "R001", "record");

        ArgumentCaptor<ConsultRecord> recordCaptor = ArgumentCaptor.forClass(ConsultRecord.class);
        verify(mapper).insertRecord(recordCaptor.capture());
        ConsultRecord inserted = recordCaptor.getValue();
        assertThat(inserted.getDoctorName()).isEqualTo("Dr. Li");
        assertThat(inserted.getCreateTime()).isNotNull();
        assertThat(inserted.getPatientName()).isEqualTo("Alice");
        assertThat(inserted.getDescription()).isEqualTo("record");
        assertThat(inserted.getPayStatus()).isEqualTo("PAID");
        verify(mapper).markInProgress("R001");
    }

    @Test
    void getListPassesReportReturnedOnlyFilterToMapper() {
        service.getList("D001", "", "", true, 2, 10);

        verify(mapper).findList("D001", "", "", true, 10, 10);
    }

    @Test
    void getListDecoratesEntryStateFromUnifiedConsultAccessRules() {
        ConsultRecord completed = consult("D001", "COMPLETED");
        completed.setVisitDate(LocalDate.now().minusDays(1));

        ConsultRecord legacy = consult("D001", "PENDING");
        legacy.setVisitDate(null);

        ConsultRecord todayPending = consult("D001", "IN_PROGRESS");
        todayPending.setVisitDate(LocalDate.now());

        ConsultRecord historicalPending = consult("D001", "PENDING");
        historicalPending.setVisitDate(LocalDate.now().minusDays(1));

        when(mapper.findList("D001", "", "", false, 0, 10))
                .thenReturn(List.of(completed, legacy, todayPending, historicalPending));

        List<ConsultRecord> records =
                service.getList("D001", "", "", false, 1, 10);

        assertThat(records).hasSize(4);
        assertThat(records.get(0).getEntryAllowed()).isTrue();
        assertThat(records.get(0).getEntryActionText()).isEqualTo("查看");
        assertThat(records.get(0).getEntryBlockedReason()).isNull();
        assertThat(records.get(0).getReadOnly()).isTrue();

        assertThat(records.get(1).getEntryAllowed()).isFalse();
        assertThat(records.get(1).getEntryActionText()).isEqualTo("接诊");
        assertThat(records.get(1).getEntryBlockedReason()).contains("缺少就诊日期");
        assertThat(records.get(1).getReadOnly()).isFalse();

        assertThat(records.get(2).getEntryAllowed()).isTrue();
        assertThat(records.get(2).getEntryActionText()).isEqualTo("接诊");
        assertThat(records.get(2).getEntryBlockedReason()).isNull();
        assertThat(records.get(2).getReadOnly()).isFalse();

        assertThat(records.get(3).getEntryAllowed()).isFalse();
        assertThat(records.get(3).getEntryActionText()).isEqualTo("接诊");
        assertThat(records.get(3).getEntryBlockedReason()).contains("已过期");
        assertThat(records.get(3).getReadOnly()).isFalse();
    }

    @Test
    void getDetailRejectsHistoricalUnfinishedConsult() {
        ConsultRecord record = consult("D001", "IN_PROGRESS");
        record.setVisitDate(LocalDate.now().minusDays(1));
        when(mapper.findDetail("R001")).thenReturn(record);

        assertThrows(BusinessException.class,
                () -> service.getDetail("D001", "R001"));
    }

    @Test
    void getDetailAllowsHistoricalCompletedConsultAsReadOnly() {
        ConsultRecord record = consult("D001", "COMPLETED");
        record.setVisitDate(LocalDate.now().minusDays(1));
        when(mapper.findDetail("R001")).thenReturn(record);

        ConsultRecord detail = service.getDetail("D001", "R001");

        assertSame(record, detail);
        assertThat(detail.getReadOnly()).isTrue();
    }

    @Test
    void getDetailRejectsFutureConsult() {
        ConsultRecord record = consult("D001", "PENDING");
        record.setVisitDate(LocalDate.now().plusDays(1));
        when(mapper.findDetail("R001")).thenReturn(record);

        assertThrows(BusinessException.class,
                () -> service.getDetail("D001", "R001"));
    }

    @Test
    void getDetailRejectsUnfinishedConsultWithoutVisitDate() {
        ConsultRecord record = consult("D001", "IN_PROGRESS");
        record.setVisitDate(null);
        when(mapper.findDetail("R001")).thenReturn(record);

        assertThrows(BusinessException.class,
                () -> service.getDetail("D001", "R001"));
    }

    @Test
    void completeRequiresConfirmedRecord() {
        ConsultRecord record = consult("D001", "IN_PROGRESS");
        when(mapper.findDetail("R001")).thenReturn(record);

        assertThrows(BusinessException.class,
                () -> service.completeConsult("D001", "R001"));
    }

    @Test
    void completeRejectsPendingMedicalOrderItems() {
        ConsultRecord record = consult("D001", "RECORD_CONFIRMED");
        when(mapper.findDetail("R001")).thenReturn(record);
        when(mapper.countPendingMedicalOrderItems("R001")).thenReturn(1);

        assertThrows(BusinessException.class,
                () -> service.completeConsult("D001", "R001"));

        verify(mapper, never()).completeConsult("R001");
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
        record.setVisitDate(LocalDate.now());
        when(mapper.findDetail("R001")).thenReturn(record);
        when(mapper.findMedicalItemsByNames(List.of("CT")))
                .thenReturn(List.of(Map.of(
                        "item_id", "ITEM001",
                        "item_code", "NEURO_CT_001",
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
    void createExamOrderAutoQueuesWhenDevFlagEnabled() {
        ConsultServiceImpl devService =
                new ConsultServiceImpl(mapper, paymentFeignClient, medicalOrderMapper, true);
        ConsultRecord record = consult("D001", "IN_PROGRESS");
        record.setPatientId("P001");
        record.setPatientName("Alice");
        record.setVisitDate(LocalDate.now());
        when(mapper.findDetail("R001")).thenReturn(record);
        when(mapper.findMedicalItemsByNames(List.of("CT")))
                .thenReturn(List.of(Map.of(
                        "item_id", "ITEM001",
                        "item_code", "NEURO_CT_001",
                        "item_name", "CT",
                        "item_category", "EXAM",
                        "dept_id", "DEPT001",
                        "price", new BigDecimal("280.00"))));
        when(paymentFeignClient.createPayOrder(any(UnifiedPayDto.class)))
                .thenReturn(Result.ok(new PayResultVo()));

        devService.createExamOrder("D001", "R001", "[{\"itemName\":\"CT\"}]", "NORMAL");

        ArgumentCaptor<String> orderIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(medicalOrderMapper).updatePayStatus(orderIdCaptor.capture());
        String orderId = orderIdCaptor.getValue();
        assertThat(orderId).startsWith("CHK");
        verify(medicalOrderMapper).enqueueOrder(orderId);
        verify(medicalOrderMapper).enqueueOrderItems(orderId);
    }

    @Test
    void createExamOrderStillQueuesWhenDevPaymentFails() {
        ConsultServiceImpl devService =
                new ConsultServiceImpl(mapper, paymentFeignClient, medicalOrderMapper, true);
        ConsultRecord record = consult("D001", "IN_PROGRESS");
        record.setPatientId("P001");
        record.setVisitDate(LocalDate.now());
        when(mapper.findDetail("R001")).thenReturn(record);
        when(mapper.findMedicalItemsByNames(List.of("CT")))
                .thenReturn(List.of(Map.of(
                        "item_id", "ITEM001",
                        "item_code", "NEURO_CT_001",
                        "item_name", "CT",
                        "item_category", "EXAM",
                        "dept_id", "DEPT001",
                        "price", new BigDecimal("280.00"))));
        when(paymentFeignClient.createPayOrder(any(UnifiedPayDto.class)))
                .thenThrow(new RuntimeException("payment-service down"));

        devService.createExamOrder("D001", "R001", "[{\"itemName\":\"CT\"}]", "NORMAL");

        verify(medicalOrderMapper).updatePayStatus(any());
        verify(medicalOrderMapper).enqueueOrder(any());
        verify(medicalOrderMapper).enqueueOrderItems(any());
    }

    @Test
    void createExamOrderDoesNotQueueWhenDevFlagDisabled() {
        ConsultRecord record = consult("D001", "IN_PROGRESS");
        record.setPatientId("P001");
        record.setVisitDate(LocalDate.now());
        when(mapper.findDetail("R001")).thenReturn(record);
        when(mapper.findMedicalItemsByNames(List.of("CT")))
                .thenReturn(List.of(Map.of(
                        "item_id", "ITEM001",
                        "item_code", "NEURO_CT_001",
                        "item_name", "CT",
                        "item_category", "EXAM",
                        "dept_id", "DEPT001",
                        "price", new BigDecimal("280.00"))));
        when(paymentFeignClient.createPayOrder(any(UnifiedPayDto.class)))
                .thenReturn(Result.ok(new PayResultVo()));

        service.createExamOrder("D001", "R001", "[{\"itemName\":\"CT\"}]", "NORMAL");

        verify(medicalOrderMapper, never()).updatePayStatus(any());
        verify(medicalOrderMapper, never()).enqueueOrder(any());
        verify(medicalOrderMapper, never()).enqueueOrderItems(any());
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

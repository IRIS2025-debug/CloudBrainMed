package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.doctor.dto.MedicalOrderConfirmRequest;
import com.cloudbrainmed.doctor.dto.MedicalOrderConfirmResponse;
import com.cloudbrainmed.doctor.dto.MedicalOrderItemRequest;
import com.cloudbrainmed.doctor.entity.ConsultRecord;
import com.cloudbrainmed.doctor.entity.MedicalItem;
import com.cloudbrainmed.doctor.entity.MedicalOrder;
import com.cloudbrainmed.doctor.entity.MedicalOrderItem;
import com.cloudbrainmed.doctor.mapper.ConsultMapper;
import com.cloudbrainmed.doctor.mapper.MedicalItemMapper;
import com.cloudbrainmed.doctor.mapper.MedicalOrderMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MedicalOrderServiceImplTest {

    private ConsultMapper consultMapper;
    private MedicalItemMapper medicalItemMapper;
    private MedicalOrderMapper medicalOrderMapper;
    private MedicalOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        consultMapper = mock(ConsultMapper.class);
        medicalItemMapper = mock(MedicalItemMapper.class);
        medicalOrderMapper = mock(MedicalOrderMapper.class);
        service = new MedicalOrderServiceImpl(
                consultMapper, medicalItemMapper, medicalOrderMapper,
                new ObjectMapper());
    }

    @Test
    void confirmPersistsDatabaseSnapshotsAndAiTrace() {
        ConsultRecord consult = new ConsultRecord();
        consult.setRegisterId("REG001");
        consult.setPatientId("P001");
        consult.setDoctorId("D001");
        when(consultMapper.findDetail("REG001")).thenReturn(consult);

        MedicalItem item = new MedicalItem();
        item.setItemId("ITEM001");
        item.setItemCode("CRANIAL_CT_PLAIN");
        item.setItemName("颅脑CT平扫");
        item.setItemCategory("EXAM");
        item.setDeptId("DEPT001");
        item.setPrice(new BigDecimal("280.00"));
        item.setStatus(1);
        when(medicalItemMapper.selectEnabledByCode("CRANIAL_CT_PLAIN"))
                .thenReturn(item);
        when(medicalOrderMapper.findAiRecommendationInput("AI123", "P001"))
                .thenReturn("{\"registerId\":\"REG001\"}");
        when(medicalOrderMapper.findAiRecommendationOutput("AI123", "P001"))
                .thenReturn("{\"recommendations\":[{\"itemCode\":\"CRANIAL_CT_PLAIN\"}]}");
        when(medicalOrderMapper.keepConsultInProgress("REG001", "D001"))
                .thenReturn(1);

        MedicalOrderConfirmRequest request = request(
                List.of(itemRequest("CRANIAL_CT_PLAIN", "URGENT")));
        request.setAiTraceId("AI123");
        MedicalOrderConfirmResponse response =
                service.confirm(request, "D001");

        ArgumentCaptor<MedicalOrder> orderCaptor =
                ArgumentCaptor.forClass(MedicalOrder.class);
        ArgumentCaptor<MedicalOrderItem> itemCaptor =
                ArgumentCaptor.forClass(MedicalOrderItem.class);
        verify(medicalOrderMapper).insertOrder(orderCaptor.capture());
        verify(medicalOrderMapper).insertOrderItem(itemCaptor.capture());

        assertThat(orderCaptor.getValue().getSourceType())
                .isEqualTo("AI_ASSISTED");
        assertThat(orderCaptor.getValue().getAiTraceId())
                .isEqualTo("AI123");
        assertThat(orderCaptor.getValue().getUrgencyLevel())
                .isEqualTo("URGENT");
        assertThat(itemCaptor.getValue().getItemName())
                .isEqualTo("颅脑CT平扫");
        assertThat(itemCaptor.getValue().getPrice())
                .isEqualByComparingTo("280.00");
        assertThat(response.getTotalAmount())
                .isEqualByComparingTo("280.00");
        assertThat(response.getItemCount()).isEqualTo(1);
    }

    @Test
    void confirmRejectsAnotherDoctorsConsult() {
        ConsultRecord consult = new ConsultRecord();
        consult.setRegisterId("REG001");
        consult.setDoctorId("D002");
        when(consultMapper.findDetail("REG001")).thenReturn(consult);

        assertThatThrownBy(() -> service.confirm(
                request(List.of(itemRequest(
                        "CRANIAL_CT_PLAIN", null))), "D001"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权");
        verify(medicalOrderMapper, never())
                .insertOrder(any(MedicalOrder.class));
    }

    @Test
    void confirmRejectsDuplicateItems() {
        ConsultRecord consult = new ConsultRecord();
        consult.setRegisterId("REG001");
        consult.setPatientId("P001");
        consult.setDoctorId("D001");
        when(consultMapper.findDetail("REG001")).thenReturn(consult);

        MedicalItem item = new MedicalItem();
        item.setItemCode("CRANIAL_CT_PLAIN");
        item.setItemCategory("EXAM");
        when(medicalItemMapper.selectEnabledByCode("CRANIAL_CT_PLAIN"))
                .thenReturn(item);

        assertThatThrownBy(() -> service.confirm(
                request(List.of(
                        itemRequest("CRANIAL_CT_PLAIN", null),
                        itemRequest("CRANIAL_CT_PLAIN", null))),
                "D001"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能重复");
        verify(medicalOrderMapper, never())
                .insertOrder(any(MedicalOrder.class));
    }

    @Test
    void confirmRejectsUnknownAiTrace() {
        ConsultRecord consult = new ConsultRecord();
        consult.setRegisterId("REG001");
        consult.setPatientId("P001");
        consult.setDoctorId("D001");
        when(consultMapper.findDetail("REG001")).thenReturn(consult);

        MedicalItem item = new MedicalItem();
        item.setItemCode("CRANIAL_CT_PLAIN");
        item.setItemCategory("EXAM");
        when(medicalItemMapper.selectEnabledByCode("CRANIAL_CT_PLAIN"))
                .thenReturn(item);

        MedicalOrderConfirmRequest request = request(
                List.of(itemRequest("CRANIAL_CT_PLAIN", null)));
        request.setAiTraceId("AI_UNKNOWN");

        assertThatThrownBy(() -> service.confirm(request, "D001"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在或已失效");
        verify(medicalOrderMapper, never())
                .insertOrder(any(MedicalOrder.class));
    }

    private MedicalOrderConfirmRequest request(
            List<MedicalOrderItemRequest> items) {
        MedicalOrderConfirmRequest request =
                new MedicalOrderConfirmRequest();
        request.setRegisterId("REG001");
        request.setClinicalSummary("头痛伴眩晕，排除颅内异常");
        request.setUrgencyLevel("NORMAL");
        request.setItems(items);
        return request;
    }

    private MedicalOrderItemRequest itemRequest(
            String itemCode, String urgency) {
        MedicalOrderItemRequest request =
                new MedicalOrderItemRequest();
        request.setItemCode(itemCode);
        request.setUrgencyLevel(urgency);
        return request;
    }
}

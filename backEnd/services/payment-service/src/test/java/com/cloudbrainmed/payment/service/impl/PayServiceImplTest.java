package com.cloudbrainmed.payment.service.impl;

import com.cloudbrainmed.payment.entity.Pay;
import com.cloudbrainmed.payment.mapper.PayMapper;
import com.cloudbrainmed.payment.service.MedicalOrderCallbackService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PayServiceImplTest {

    @Test
    void paySuccessEnqueuesMedicalOrderWithoutPreUpdatingBusinessStatus() {
        PayMapper payMapper = mock(PayMapper.class);
        MedicalOrderCallbackService callbackService =
                mock(MedicalOrderCallbackService.class);
        PayServiceImpl service = new PayServiceImpl(payMapper, callbackService);
        Pay pay = waitingPay("pay001", "MEDICAL", "MO001");
        when(payMapper.selectByPayId("pay001")).thenReturn(pay);
        when(payMapper.updatePaySuccess(eq("pay001"), eq("PAID"), any()))
                .thenReturn(1);
        when(callbackService.onOrderPaid("MO001")).thenReturn(true);

        boolean result = service.paySuccess("pay001");

        assertThat(result).isTrue();
        verify(payMapper, never())
                .updateBusinessPayStatus("MEDICAL", "MO001", "PAID");
        verify(callbackService).onOrderPaid("MO001");
    }

    @Test
    void paySuccessFailsWhenMedicalOrderCallbackFails() {
        PayMapper payMapper = mock(PayMapper.class);
        MedicalOrderCallbackService callbackService =
                mock(MedicalOrderCallbackService.class);
        PayServiceImpl service = new PayServiceImpl(payMapper, callbackService);
        Pay pay = waitingPay("pay006", "MEDICAL", "MO001");
        when(payMapper.selectByPayId("pay006")).thenReturn(pay);
        when(payMapper.updatePaySuccess(eq("pay006"), eq("PAID"), any()))
                .thenReturn(1);
        when(callbackService.onOrderPaid("MO001")).thenReturn(false);

        assertThatThrownBy(() -> service.paySuccess("pay006"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("medical order callback failed");
        verify(payMapper, never())
                .updateBusinessPayStatus("MEDICAL", "MO001", "PAID");
        verify(callbackService).onOrderPaid("MO001");
    }

    @Test
    void paySuccessDoesNotEnqueuePrescriptionOrder() {
        PayMapper payMapper = mock(PayMapper.class);
        MedicalOrderCallbackService callbackService =
                mock(MedicalOrderCallbackService.class);
        PayServiceImpl service = new PayServiceImpl(payMapper, callbackService);
        Pay pay = waitingPay("pay002", "PRESCRIPTION", "PRE001");
        when(payMapper.selectByPayId("pay002")).thenReturn(pay);
        when(payMapper.updatePaySuccess(eq("pay002"), eq("PAID"), any()))
                .thenReturn(1);
        when(payMapper.updateBusinessPayStatus("PRESCRIPTION", "PRE001", "PAID"))
                .thenReturn(1);

        boolean result = service.paySuccess("pay002");

        assertThat(result).isTrue();
        verify(callbackService, never()).onOrderPaid("PRE001");
    }

    @Test
    void paySuccessFailsWhenPrescriptionStatusDoesNotSync() {
        PayMapper payMapper = mock(PayMapper.class);
        MedicalOrderCallbackService callbackService =
                mock(MedicalOrderCallbackService.class);
        PayServiceImpl service = new PayServiceImpl(payMapper, callbackService);
        Pay pay = waitingPay("pay003", "PRESCRIPTION", "PRE001");
        when(payMapper.selectByPayId("pay003")).thenReturn(pay);
        when(payMapper.updatePaySuccess(eq("pay003"), eq("PAID"), any()))
                .thenReturn(1);
        when(payMapper.updateBusinessPayStatus("PRESCRIPTION", "PRE001", "PAID"))
                .thenReturn(0);

        assertThatThrownBy(() -> service.paySuccess("pay003"))
                .isInstanceOf(RuntimeException.class);
        verify(callbackService, never()).onOrderPaid("PRE001");
    }

    @Test
    void paySuccessDoesNotSyncBusinessStatusWhenPayRowWasNotUpdated() {
        PayMapper payMapper = mock(PayMapper.class);
        MedicalOrderCallbackService callbackService =
                mock(MedicalOrderCallbackService.class);
        PayServiceImpl service = new PayServiceImpl(payMapper, callbackService);
        Pay pay = waitingPay("pay004", "MEDICAL", "MO001");
        when(payMapper.selectByPayId("pay004")).thenReturn(pay);
        when(payMapper.updatePaySuccess(eq("pay004"), eq("PAID"), any()))
                .thenReturn(0);

        assertThatThrownBy(() -> service.paySuccess("pay004"))
                .isInstanceOf(RuntimeException.class);
        verify(payMapper, never())
                .updateBusinessPayStatus("MEDICAL", "MO001", "PAID");
        verify(callbackService, never()).onOrderPaid("MO001");
    }

    @Test
    void cancelPayDoesNotSyncBusinessStatusWhenPayRowWasNotUpdated() {
        PayMapper payMapper = mock(PayMapper.class);
        MedicalOrderCallbackService callbackService =
                mock(MedicalOrderCallbackService.class);
        PayServiceImpl service = new PayServiceImpl(payMapper, callbackService);
        Pay pay = waitingPay("pay005", "MEDICAL", "MO001");
        when(payMapper.selectByPayId("pay005")).thenReturn(pay);
        when(payMapper.updatePayStatusFrom("pay005", "CANCELLED", "WAITING"))
                .thenReturn(0);

        assertThatThrownBy(() -> service.cancelPay("pay005"))
                .isInstanceOf(RuntimeException.class);
        verify(payMapper).updatePayStatusFrom("pay005", "CANCELLED", "WAITING");
        verify(payMapper, never())
                .updateBusinessPayStatus("MEDICAL", "MO001", "CANCELLED");
        verify(callbackService, never()).onOrderPaid("MO001");
    }

    @Test
    void refundPayDoesNotSyncBusinessStatusWhenPayRowWasNotUpdated() {
        PayMapper payMapper = mock(PayMapper.class);
        MedicalOrderCallbackService callbackService =
                mock(MedicalOrderCallbackService.class);
        PayServiceImpl service = new PayServiceImpl(payMapper, callbackService);
        Pay pay = waitingPay("pay007", "PRESCRIPTION", "PRE001");
        pay.setPayStatus("PAID");
        when(payMapper.selectByPayId("pay007")).thenReturn(pay);
        when(payMapper.updatePayStatusFrom("pay007", "REFUNDED", "PAID"))
                .thenReturn(0);

        assertThatThrownBy(() -> service.refundPay("pay007"))
                .isInstanceOf(RuntimeException.class);
        verify(payMapper).updatePayStatusFrom("pay007", "REFUNDED", "PAID");
        verify(payMapper, never())
                .updateBusinessPayStatus("PRESCRIPTION", "PRE001", "REFUNDED");
        verify(callbackService, never()).onOrderPaid("PRE001");
    }

    private Pay waitingPay(String payId, String orderType, String businessId) {
        Pay pay = new Pay();
        pay.setPayId(payId);
        pay.setOrderType(orderType);
        pay.setBusinessId(businessId);
        pay.setPayStatus("WAITING");
        return pay;
    }
}

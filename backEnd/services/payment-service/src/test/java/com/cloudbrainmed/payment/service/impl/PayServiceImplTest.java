package com.cloudbrainmed.payment.service.impl;

import com.cloudbrainmed.payment.entity.Pay;
import com.cloudbrainmed.payment.mapper.PayMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PayServiceImplTest {

    @Test
    void paySuccessMarksMedicalOrderPaid() {
        PayMapper payMapper = mock(PayMapper.class);
        PayServiceImpl service = new PayServiceImpl(payMapper);
        Pay pay = new Pay();
        pay.setPayId("pay001");
        pay.setOrderType("MEDICAL");
        pay.setBusinessId("MO001");
        pay.setPayStatus("WAITING");
        when(payMapper.selectByPayId("pay001")).thenReturn(pay);
        when(payMapper.updatePaySuccess(org.mockito.ArgumentMatchers.eq("pay001"),
                org.mockito.ArgumentMatchers.eq("PAID"),
                org.mockito.ArgumentMatchers.any())).thenReturn(1);
        when(payMapper.updateBusinessPayStatus("MEDICAL", "MO001", "PAID"))
                .thenReturn(1);

        boolean result = service.paySuccess("pay001");

        assertThat(result).isTrue();
        verify(payMapper).updateBusinessPayStatus("MEDICAL", "MO001", "PAID");
    }

    @Test
    void paySuccessFailsWhenPrescriptionStatusDoesNotSync() {
        PayMapper payMapper = mock(PayMapper.class);
        PayServiceImpl service = new PayServiceImpl(payMapper);
        Pay pay = new Pay();
        pay.setPayId("pay002");
        pay.setOrderType("PRESCRIPTION");
        pay.setBusinessId("PRE001");
        pay.setPayStatus("WAITING");
        when(payMapper.selectByPayId("pay002")).thenReturn(pay);
        when(payMapper.updatePaySuccess(org.mockito.ArgumentMatchers.eq("pay002"),
                org.mockito.ArgumentMatchers.eq("PAID"),
                org.mockito.ArgumentMatchers.any())).thenReturn(1);
        when(payMapper.updateBusinessPayStatus("PRESCRIPTION", "PRE001", "PAID"))
                .thenReturn(0);

        assertThatThrownBy(() -> service.paySuccess("pay002"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("同步失败");
    }

    @Test
    void paySuccessDoesNotSyncBusinessStatusWhenPayRowWasNotUpdated() {
        PayMapper payMapper = mock(PayMapper.class);
        PayServiceImpl service = new PayServiceImpl(payMapper);
        Pay pay = new Pay();
        pay.setPayId("pay003");
        pay.setOrderType("MEDICAL");
        pay.setBusinessId("MO001");
        pay.setPayStatus("WAITING");
        when(payMapper.selectByPayId("pay003")).thenReturn(pay);
        when(payMapper.updatePaySuccess(org.mockito.ArgumentMatchers.eq("pay003"),
                org.mockito.ArgumentMatchers.eq("PAID"),
                org.mockito.ArgumentMatchers.any())).thenReturn(0);

        assertThatThrownBy(() -> service.paySuccess("pay003"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("支付状态更新失败");
        verify(payMapper, never())
                .updateBusinessPayStatus("MEDICAL", "MO001", "PAID");
    }

    @Test
    void cancelPayDoesNotSyncBusinessStatusWhenPayRowWasNotUpdated() {
        PayMapper payMapper = mock(PayMapper.class);
        PayServiceImpl service = new PayServiceImpl(payMapper);
        Pay pay = new Pay();
        pay.setPayId("pay004");
        pay.setOrderType("MEDICAL");
        pay.setBusinessId("MO001");
        pay.setPayStatus("WAITING");
        when(payMapper.selectByPayId("pay004")).thenReturn(pay);
        when(payMapper.updatePayStatusFrom("pay004", "CANCELLED", "WAITING"))
                .thenReturn(0);

        assertThatThrownBy(() -> service.cancelPay("pay004"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("支付状态更新失败");
        verify(payMapper).updatePayStatusFrom("pay004", "CANCELLED", "WAITING");
        verify(payMapper, never())
                .updateBusinessPayStatus("MEDICAL", "MO001", "CANCELLED");
    }

    @Test
    void refundPayDoesNotSyncBusinessStatusWhenPayRowWasNotUpdated() {
        PayMapper payMapper = mock(PayMapper.class);
        PayServiceImpl service = new PayServiceImpl(payMapper);
        Pay pay = new Pay();
        pay.setPayId("pay005");
        pay.setOrderType("PRESCRIPTION");
        pay.setBusinessId("PRE001");
        pay.setPayStatus("PAID");
        when(payMapper.selectByPayId("pay005")).thenReturn(pay);
        when(payMapper.updatePayStatusFrom("pay005", "REFUNDED", "PAID"))
                .thenReturn(0);

        assertThatThrownBy(() -> service.refundPay("pay005"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("支付状态更新失败");
        verify(payMapper).updatePayStatusFrom("pay005", "REFUNDED", "PAID");
        verify(payMapper, never())
                .updateBusinessPayStatus("PRESCRIPTION", "PRE001", "REFUNDED");
    }
}

package com.cloudbrainmed.ai.service.impl;

import com.cloudbrainmed.admin.entity.DoctorSchedule;
import com.cloudbrainmed.ai.dto.AiScheduleItem;
import com.cloudbrainmed.ai.dto.AiSchedulePublishRequest;
import com.cloudbrainmed.api.feign.AdminFeignClient;
import com.cloudbrainmed.common.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiScheduleServiceImplTest {

    private AdminFeignClient adminFeignClient;
    private AiScheduleServiceImpl service;

    @BeforeEach
    void setUp() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(mock(ChatClient.class, RETURNS_DEEP_STUBS));
        adminFeignClient = mock(AdminFeignClient.class);
        service = new AiScheduleServiceImpl(
                builder, mock(com.fasterxml.jackson.databind.ObjectMapper.class),
                adminFeignClient, "deepseek-chat");
    }

    @Test
    void publishPropagatesAdminTokenToAdminService() {
        String token = "admin-token";
        AiSchedulePublishRequest request = new AiSchedulePublishRequest();
        request.setItems(List.of(scheduleItem()));
        when(adminFeignClient.checkConflict(eq(token), any(DoctorSchedule.class)))
                .thenReturn(Result.ok(false));
        when(adminFeignClient.batchCreateSchedules(eq(token), anyList()))
                .thenReturn(Result.ok(List.of(new DoctorSchedule())));

        var response = service.publish(request, "A001", token);

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        verify(adminFeignClient).checkConflict(eq(token), any(DoctorSchedule.class));
        verify(adminFeignClient).batchCreateSchedules(eq(token), anyList());
    }

    private AiScheduleItem scheduleItem() {
        AiScheduleItem item = new AiScheduleItem();
        item.setDoctorId("D001");
        item.setDoctorName("Doctor A");
        item.setDeptId("DEPT001");
        item.setWorkDate(LocalDate.now().plusDays(1));
        item.setStartTime(LocalTime.of(9, 0));
        item.setEndTime(LocalTime.of(10, 0));
        item.setMaxNum(10);
        item.setPrice(new BigDecimal("20.00"));
        item.setRoom("Room 1");
        return item;
    }
}

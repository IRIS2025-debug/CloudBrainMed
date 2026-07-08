package com.cloudbrainmed.ai.controller;

import com.cloudbrainmed.ai.dto.AiSchedulePublishRequest;
import com.cloudbrainmed.ai.dto.AiSchedulePublishResponse;
import com.cloudbrainmed.ai.service.AiScheduleService;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiScheduleControllerTest {

    @Test
    void publishPassesResolvedAdminTokenToService() {
        AiScheduleService service = mock(AiScheduleService.class);
        AiScheduleController controller = new AiScheduleController(service);
        AiSchedulePublishRequest request = new AiSchedulePublishRequest();
        String token = DoctorJwtUtil.createToken("A001", "13700000001", 3);
        when(service.publish(same(request), org.mockito.ArgumentMatchers.eq("A001"),
                org.mockito.ArgumentMatchers.eq(token)))
                .thenReturn(new AiSchedulePublishResponse());

        controller.publish(Map.of(HttpHeaders.AUTHORIZATION, "Bearer " + token), request);

        verify(service).publish(request, "A001", token);
    }
}

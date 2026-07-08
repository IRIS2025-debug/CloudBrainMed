package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.cloudbrainmed.doctor.service.TaskSchedulerService;
import com.cloudbrainmed.doctor.vo.DoctorTaskDetailVo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskControllerTest {

    private final TaskSchedulerService taskSchedulerService =
            mock(TaskSchedulerService.class);
    private final TaskController controller =
            new TaskController(taskSchedulerService);

    @Test
    void detailRejectsLabDoctorOpeningExamTask() {
        String token = DoctorJwtUtil.createToken("D003", "11111111111", 2, 3);
        DoctorTaskDetailVo detail = new DoctorTaskDetailVo();
        detail.setOrderItemId("MOI_EXAM");
        detail.setItemCategory("EXAM");
        when(taskSchedulerService.getTaskDetail("MOI_EXAM", "D003", 3))
                .thenReturn(detail);

        assertThatThrownBy(() -> controller.detail(token, "MOI_EXAM"))
                .hasMessageContaining("无权");
    }

    @Test
    void detailRejectsExamDoctorOpeningLabTask() {
        String token = DoctorJwtUtil.createToken("D002", "11111111111", 2, 2);
        DoctorTaskDetailVo detail = new DoctorTaskDetailVo();
        detail.setOrderItemId("MOI_LAB");
        detail.setItemCategory("LAB");
        when(taskSchedulerService.getTaskDetail("MOI_LAB", "D002", 2))
                .thenReturn(detail);

        assertThatThrownBy(() -> controller.detail(token, "MOI_LAB"))
                .hasMessageContaining("无权");
    }
}

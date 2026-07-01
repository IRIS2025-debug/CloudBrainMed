package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.cloudbrainmed.doctor.mapper.MedicineOptionMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DoctorMedicineControllerTest {

    private final MedicineOptionMapper mapper = mock(MedicineOptionMapper.class);
    private final DoctorMedicineController controller = new DoctorMedicineController(mapper);

    @Test
    void listRejectsInvalidToken() {
        assertThatThrownBy(() -> controller.list("not-a-jwt", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("医生登录凭证无效");

        verify(mapper, never()).list(null);
    }

    @Test
    void listRequiresDoctorToken() {
        String token = DoctorJwtUtil.createToken("D001", "11111111111", 2);

        controller.list(token, "阿莫西林");

        verify(mapper).list("阿莫西林");
    }
}

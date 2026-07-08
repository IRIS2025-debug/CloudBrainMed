package com.cloudbrainmed.doctor.mapper;

import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class MedicalOrderMapperSqlTest {

    @Test
    void inspectionOrderListMapsAssignedDoctorIdForActionFlags() throws Exception {
        Method method = MedicalOrderMapper.class.getMethod("selectAllLabOrders");

        Select select = method.getAnnotation(Select.class);
        Results results = method.getAnnotation(Results.class);

        assertThat(String.join(" ", select.value()))
                .contains("moi.assigned_doctor_id");
        assertThat(Arrays.stream(results.value()))
                .anyMatch(this::mapsAssignedDoctorId);
    }

    private boolean mapsAssignedDoctorId(Result result) {
        return "assigned_doctor_id".equals(result.column())
                && "assignedDoctorId".equals(result.property());
    }
}

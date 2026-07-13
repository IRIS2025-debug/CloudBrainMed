package com.cloudbrainmed.doctor.mapper;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class ConsultMapperTest {

    @Test
    void consultListOnlyReturnsPaidRegistrations() throws NoSuchMethodException {
        Method findList = ConsultMapper.class.getMethod(
                "findList", String.class, String.class, String.class,
                boolean.class, int.class, int.class);
        Select select = findList.getAnnotation(Select.class);

        assertThat(String.join(" ", select.value()))
                .contains("AND r.pay_status = 'PAID'");
    }
}

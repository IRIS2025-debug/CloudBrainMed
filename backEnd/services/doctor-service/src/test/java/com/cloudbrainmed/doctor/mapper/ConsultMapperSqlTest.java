package com.cloudbrainmed.doctor.mapper;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ConsultMapperSqlTest {

    @Test
    void consultListUsesMedicalRecordVisitDateWhenRegistrationVisitDateIsMissing() {
        String sql = mapperSql();

        assertThat(sql)
                .contains("left join ( select register_id, max(visit_date) as visit_date from register_report group by register_id) rr")
                .contains("coalesce(r.visit_date, rr.visit_date, case when r.consult_status = 'completed' then r.create_time::date end) as effective_visit_date")
                .contains("and coalesce(r.visit_date, rr.visit_date, case when r.consult_status = 'completed' then r.create_time::date end) = #{date}::date");
    }

    @Test
    void consultDetailUsesMedicalRecordVisitDateWhenRegistrationVisitDateIsMissing() {
        String sql = mapperSql();

        assertThat(sql)
                .contains("coalesce(r.visit_date, m.visit_date, case when r.consult_status = 'completed' then r.create_time::date end) as effective_visit_date");
    }

    @Test
    void completeConsultPendingItemCountRequiresCompletedItemsAndPublishedReports() {
        String sql = mapperSql();

        assertThat(sql)
                .contains("moi.status <> 'cancelled'")
                .contains("moi.status <> 'completed'")
                .contains("from medical_report mr")
                .contains("mr.status = 'published'");
    }

    private String mapperSql() {
        return Arrays.stream(ConsultMapper.class.getDeclaredMethods())
                .map(this::selectSql)
                .collect(Collectors.joining("\n"))
                .toLowerCase(Locale.ROOT);
    }

    private String selectSql(Method method) {
        Select select = method.getAnnotation(Select.class);
        return select == null ? "" : String.join(" ", select.value());
    }
}

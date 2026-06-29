package com.cloudbrainmed.admin.service.impl;

import com.cloudbrainmed.admin.dto.AiScheduleDto;
import com.cloudbrainmed.admin.entity.ScheduleRule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleGeneratorTest {

    private final ScheduleGenerator generator = new ScheduleGenerator();

    @Test
    void balancesDoctorsWhenOnlyOneRoomIsAvailable() {
        AiScheduleDto request = request(
                LocalDate.of(2026, 6, 15), List.of("A101"));
        List<ScheduleRule> rules = List.of(
                rule("DOC001", 1, LocalTime.of(9, 0),
                        LocalTime.of(11, 0), 4),
                rule("DOC002", 1, LocalTime.of(9, 0),
                        LocalTime.of(11, 0), 4));

        ScheduleGenerator.GenerationResult result =
                generator.generate(request, rules, "SPL001");

        assertThat(result.items()).hasSize(2);
        assertThat(result.items())
                .extracting(item -> item.getDoctorId())
                .containsExactly("DOC001", "DOC002");
        assertThat(result.items())
                .extracting(item -> item.getRoom())
                .containsOnly("A101");
        assertThat(result.coverageRate())
                .isEqualByComparingTo("1.0000");
    }

    @Test
    void doesNotDoubleBookRoomForOverlappingDifferentSlots() {
        AiScheduleDto request = request(
                LocalDate.of(2026, 6, 15), List.of("A101"));
        List<ScheduleRule> rules = List.of(
                rule("DOC001", 1, LocalTime.of(9, 0),
                        LocalTime.of(10, 0), 4),
                rule("DOC002", 1, LocalTime.of(9, 30),
                        LocalTime.of(10, 30), 4));

        ScheduleGenerator.GenerationResult result =
                generator.generate(request, rules, "SPL001");

        assertThat(result.items()).hasSize(1);
        assertThat(result.coverageRate())
                .isEqualByComparingTo("0.5000");
        assertThat(result.warnings()).hasSize(1);
    }

    private AiScheduleDto request(
            LocalDate date, List<String> rooms) {
        AiScheduleDto request = new AiScheduleDto();
        request.setPeriodStart(date);
        request.setPeriodEnd(date);
        request.setDeptId("DEPT001");
        request.setStrategy("WORKLOAD_BALANCED");
        request.setSlotMinutes(60);
        request.setDefaultPrice(BigDecimal.valueOf(30));
        request.setRooms(rooms);
        return request;
    }

    private ScheduleRule rule(
            String doctorId,
            int dayOfWeek,
            LocalTime start,
            LocalTime end,
            int preferredLevel) {
        ScheduleRule rule = new ScheduleRule();
        rule.setRuleId("RULE-" + doctorId);
        rule.setDoctorId(doctorId);
        rule.setDoctorName(doctorId);
        rule.setDeptId("DEPT001");
        rule.setDayOfWeek(dayOfWeek);
        rule.setStartTime(start);
        rule.setEndTime(end);
        rule.setMaxPatients(8);
        rule.setPreferredLevel(preferredLevel);
        rule.setValidFrom(LocalDate.of(2026, 1, 1));
        rule.setValidTo(LocalDate.of(2026, 12, 31));
        return rule;
    }
}

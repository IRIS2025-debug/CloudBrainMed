package com.cloudbrainmed.admin.service.impl;

import com.cloudbrainmed.admin.dto.AiScheduleDto;
import com.cloudbrainmed.admin.entity.SchedulePlanItem;
import com.cloudbrainmed.admin.entity.ScheduleRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ScheduleGenerator {

    public GenerationResult generate(
            AiScheduleDto request,
            List<ScheduleRule> rules,
            String planId) {
        Map<SlotKey, List<Candidate>> candidatesBySlot =
                expandCandidates(request, rules);
        List<SchedulePlanItem> items = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<String, Long> doctorAssignedMinutes = new HashMap<>();
        Map<String, List<Interval>> doctorBookings = new HashMap<>();
        Map<String, List<Interval>> roomBookings = new HashMap<>();
        int coveredSlots = 0;

        for (Map.Entry<SlotKey, List<Candidate>> entry
                : candidatesBySlot.entrySet()) {
            SlotKey slot = entry.getKey();
            List<Candidate> ranked = new ArrayList<>(entry.getValue());
            ranked.sort(candidateComparator(
                    request.getStrategy(), doctorAssignedMinutes));
            int assignedInSlot = 0;

            for (Candidate candidate : ranked) {
                String room = firstAvailableRoom(
                        request.getRooms(), slot, roomBookings);
                if (room == null) {
                    break;
                }
                if (overlaps(doctorBookings.get(candidate.rule().getDoctorId()),
                        slot)) {
                    continue;
                }
                SchedulePlanItem item = toItem(
                        request, planId, slot, candidate, room,
                        doctorAssignedMinutes.getOrDefault(
                                candidate.rule().getDoctorId(), 0L));
                items.add(item);
                reserve(roomBookings, room, slot);
                reserve(doctorBookings, candidate.rule().getDoctorId(), slot);
                long minutes = Duration.between(
                        slot.startTime(), slot.endTime()).toMinutes();
                doctorAssignedMinutes.merge(
                        candidate.rule().getDoctorId(), minutes, Long::sum);
                assignedInSlot++;
            }
            if (assignedInSlot > 0) {
                coveredSlots++;
            }
            if (assignedInSlot < ranked.size()) {
                warnings.add(slot.workDate() + " "
                        + slot.startTime() + "-" + slot.endTime()
                        + " 有 " + (ranked.size() - assignedInSlot)
                        + " 个医生时段因诊室或时间冲突未排入");
            }
        }

        BigDecimal coverage = candidatesBySlot.isEmpty()
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(coveredSlots)
                    .divide(BigDecimal.valueOf(candidatesBySlot.size()),
                            4, RoundingMode.HALF_UP);
        return new GenerationResult(items, warnings, coverage);
    }

    private Map<SlotKey, List<Candidate>> expandCandidates(
            AiScheduleDto request,
            List<ScheduleRule> rules) {
        Map<SlotKey, List<Candidate>> result = new LinkedHashMap<>();
        LocalDate date = request.getPeriodStart();
        while (!date.isAfter(request.getPeriodEnd())) {
            for (ScheduleRule rule : rules) {
                if (rule.getDayOfWeek() != date.getDayOfWeek().getValue()
                        || date.isBefore(rule.getValidFrom())
                        || date.isAfter(rule.getValidTo())) {
                    continue;
                }
                LocalTime start = rule.getStartTime();
                while (!start.plusMinutes(request.getSlotMinutes())
                        .isAfter(rule.getEndTime())) {
                    LocalTime end =
                            start.plusMinutes(request.getSlotMinutes());
                    SlotKey key = new SlotKey(date, start, end);
                    result.computeIfAbsent(key, ignored -> new ArrayList<>())
                            .add(new Candidate(rule));
                    start = end;
                }
            }
            date = date.plusDays(1);
        }
        return result;
    }

    private Comparator<Candidate> candidateComparator(
            String strategy,
            Map<String, Long> assignedMinutes) {
        Comparator<Candidate> workload = Comparator.comparingLong(
                candidate -> assignedMinutes.getOrDefault(
                        candidate.rule().getDoctorId(), 0L));
        Comparator<Candidate> preference = Comparator.comparingInt(
                (Candidate candidate) ->
                        candidate.rule().getPreferredLevel()).reversed();
        Comparator<Candidate> capacity = Comparator.comparingInt(
                (Candidate candidate) ->
                        candidate.rule().getMaxPatients()).reversed();

        Comparator<Candidate> comparator =
                "PREFERENCE_FIRST".equalsIgnoreCase(strategy)
                        ? preference.thenComparing(workload)
                        : "CAPACITY_FIRST".equalsIgnoreCase(strategy)
                            ? capacity.thenComparing(workload)
                            : workload.thenComparing(preference);
        return comparator.thenComparing(
                candidate -> candidate.rule().getDoctorId());
    }

    private SchedulePlanItem toItem(
            AiScheduleDto request,
            String planId,
            SlotKey slot,
            Candidate candidate,
            String room,
            long assignedMinutes) {
        ScheduleRule rule = candidate.rule();
        long ruleMinutes =
                Duration.between(rule.getStartTime(), rule.getEndTime())
                        .toMinutes();
        int maxNum = Math.max(1, (int) Math.ceil(
                rule.getMaxPatients() * request.getSlotMinutes()
                        / (double) ruleMinutes));
        BigDecimal score = BigDecimal.valueOf(
                rule.getPreferredLevel() * 20.0
                        + rule.getMaxPatients()
                        - assignedMinutes / 60.0);

        SchedulePlanItem item = new SchedulePlanItem();
        item.setPlanItemId(newId("SPI"));
        item.setPlanId(planId);
        item.setDoctorId(rule.getDoctorId());
        item.setDoctorName(rule.getDoctorName());
        item.setDeptId(rule.getDeptId());
        item.setWorkDate(slot.workDate());
        item.setStartTime(slot.startTime());
        item.setEndTime(slot.endTime());
        item.setMaxNum(maxNum);
        item.setPrice(request.getDefaultPrice());
        item.setRoom(room);
        item.setScore(score.setScale(4, RoundingMode.HALF_UP));
        item.setConflictFlag(0);
        item.setCreateTime(LocalDateTime.now());
        return item;
    }

    private String firstAvailableRoom(
            List<String> rooms,
            SlotKey slot,
            Map<String, List<Interval>> bookings) {
        return rooms.stream()
                .filter(room -> !overlaps(bookings.get(room), slot))
                .findFirst()
                .orElse(null);
    }

    private boolean overlaps(List<Interval> intervals, SlotKey slot) {
        if (intervals == null) {
            return false;
        }
        return intervals.stream().anyMatch(interval ->
                interval.workDate().equals(slot.workDate())
                && interval.startTime().isBefore(slot.endTime())
                && interval.endTime().isAfter(slot.startTime()));
    }

    private void reserve(
            Map<String, List<Interval>> bookings,
            String key,
            SlotKey slot) {
        bookings.computeIfAbsent(key, ignored -> new ArrayList<>())
                .add(new Interval(
                        slot.workDate(), slot.startTime(), slot.endTime()));
    }

    private String newId(String prefix) {
        return prefix + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 29);
    }

    public record GenerationResult(
            List<SchedulePlanItem> items,
            List<String> warnings,
            BigDecimal coverageRate) {
    }

    private record Candidate(ScheduleRule rule) {
    }

    private record SlotKey(
            LocalDate workDate,
            LocalTime startTime,
            LocalTime endTime) {
    }

    private record Interval(
            LocalDate workDate,
            LocalTime startTime,
            LocalTime endTime) {
    }
}

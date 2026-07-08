package com.cloudbrainmed.ai.service.impl;

import com.cloudbrainmed.admin.dto.ScheduleSaveDto;
import com.cloudbrainmed.admin.entity.DoctorSchedule;
import com.cloudbrainmed.ai.dto.AiScheduleConflictCheckRequest;
import com.cloudbrainmed.ai.dto.AiScheduleGenerateRequest;
import com.cloudbrainmed.ai.dto.AiScheduleGenerateResponse;
import com.cloudbrainmed.ai.dto.AiScheduleItem;
import com.cloudbrainmed.ai.dto.AiSchedulePublishRequest;
import com.cloudbrainmed.ai.dto.AiSchedulePublishResponse;
import com.cloudbrainmed.ai.dto.AiScheduleTimeWindow;
import com.cloudbrainmed.ai.service.AiScheduleService;
import com.cloudbrainmed.api.feign.AdminFeignClient;
import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.result.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class AiScheduleServiceImpl implements AiScheduleService {

    private static final int MAX_GENERATED_ITEMS = 100;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final AdminFeignClient adminFeignClient;
    private final String modelName;

    public AiScheduleServiceImpl(
            ChatClient.Builder chatClientBuilder,
            ObjectMapper objectMapper,
            AdminFeignClient adminFeignClient,
            @Value("${spring.ai.openai.chat.options.model:deepseek-v4-flash}")
            String modelName) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
        this.adminFeignClient = adminFeignClient;
        this.modelName = modelName;
    }

    @Override
    public AiScheduleGenerateResponse preview(
            AiScheduleGenerateRequest request, String adminId, String adminToken) {
        List<String> warnings = new ArrayList<>();
        List<DoctorSchedule> existingSchedules =
                fetchExistingSchedules(request, adminToken, warnings);

        AiScheduleGenerateResponse response;
        try {
            String reply = chatClient.prompt(new Prompt(buildMessages(
                    request, existingSchedules))).call().content();
            response = parseReply(reply);
            response.setFallback(false);
            response.setStatus("SUCCESS");
        } catch (Exception exception) {
            response = buildRuleBasedFallback(request);
            response.setFallback(true);
            response.setStatus("FALLBACK");
            warnings.add("AI model is unavailable; generated a rule-based schedule draft.");
            warnings.add("Error type: " + exception.getClass().getSimpleName());
        }

        normalizeResponse(response, request);
        response.getWarnings().addAll(0, warnings);
        response.setModelVersion(modelName);
        markConflicts(response.getItems(), adminToken, response.getWarnings());
        return response;
    }

    @Override
    public AiScheduleGenerateResponse checkConflicts(
            AiScheduleConflictCheckRequest request, String adminToken) {
        AiScheduleGenerateResponse response = new AiScheduleGenerateResponse();
        response.setStatus("SUCCESS");
        response.setModelVersion(modelName);
        response.setSummary("Conflict check completed.");
        response.setItems(new ArrayList<>(nullToEmpty(request.getItems())));
        markConflicts(response.getItems(), adminToken, response.getWarnings());
        return response;
    }

    @Override
    public AiSchedulePublishResponse publish(
            AiSchedulePublishRequest request, String adminId, String adminToken) {
        AiSchedulePublishResponse response = new AiSchedulePublishResponse();
        List<AiScheduleItem> items = new ArrayList<>(
                nullToEmpty(request.getItems()));
        response.setSubmittedCount(items.size());

        markConflicts(items, adminToken, response.getWarnings());
        List<ScheduleSaveDto> publishable = items.stream()
                .filter(item -> !item.isConflict())
                .map(this::toScheduleSaveDto)
                .toList();
        if (publishable.isEmpty()) {
            response.setStatus("FAILED");
            response.getWarnings().add(
                    "No publishable schedules; resolve conflicts first.");
            return response;
        }

        Result<List<DoctorSchedule>> result =
                adminFeignClient.batchCreateSchedules(adminToken, publishable);
        if (!ResultCode.SUCCESS.equals(result.getCode())) {
            response.setStatus("FAILED");
            response.getWarnings().add(valueOrDefault(
                    result.getMsg(), "admin-service failed to create schedules."));
            return response;
        }

        List<DoctorSchedule> created = result.getData() == null
                ? List.of() : result.getData();
        response.setCreatedSchedules(created);
        response.setCreatedCount(created.size());
        response.setStatus(created.size() == publishable.size()
                ? "SUCCESS" : "PARTIAL_SUCCESS");
        if (created.size() < publishable.size()) {
            response.getWarnings().add(
                    "Some schedules were not created by admin-service validation.");
        }
        return response;
    }

    private List<Message> buildMessages(
            AiScheduleGenerateRequest request,
            List<DoctorSchedule> existingSchedules) {
        String systemPrompt = """
            You are an AI scheduling assistant for a hospital administrator.
            Generate a doctor schedule draft that can be reviewed before publishing.
            Use only the provided doctor, department, date range, time windows,
            rooms, default quota and price. Do not invent doctors or departments.
            Return valid JSON only, without markdown.
            Required JSON shape:
            {
              "summary": "schedule strategy summary",
              "items": [{
                "doctorId": "doctor id",
                "doctorName": "doctor name",
                "deptId": "department id",
                "workDate": "yyyy-MM-dd",
                "startTime": "HH:mm:ss",
                "endTime": "HH:mm:ss",
                "maxNum": 30,
                "price": 0.00,
                "room": "room"
              }],
              "warnings": ["issues for the administrator"],
              "optimizationReasons": ["why this schedule was chosen"]
            }
            """;

        String userPrompt = """
            doctorId: %s
            doctorName: %s
            deptId: %s
            period: %s to %s
            adminRequirement: %s
            defaultMaxNum: %s
            defaultPrice: %s
            rooms: %s
            timeWindows: %s
            unavailableDates: %s
            existingSchedules: %s
            """.formatted(
                request.getDoctorId(),
                request.getDoctorName(),
                request.getDeptId(),
                request.getPeriodStart(),
                request.getPeriodEnd(),
                valueOrDefault(request.getRequirement(), ""),
                request.getDefaultMaxNum(),
                request.getDefaultPrice(),
                toJson(request.getRooms()),
                toJson(effectiveWindows(request)),
                toJson(request.getUnavailableDates()),
                toJson(existingSchedules));
        return List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt));
    }

    private AiScheduleGenerateResponse parseReply(String reply)
            throws Exception {
        if (!StringUtils.hasText(reply)) {
            throw new IllegalStateException("AI response is empty.");
        }
        int start = reply.indexOf('{');
        int end = reply.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException(
                    "AI response is not a valid JSON object.");
        }
        return objectMapper.readValue(
                reply.substring(start, end + 1),
                AiScheduleGenerateResponse.class);
    }

    private void normalizeResponse(
            AiScheduleGenerateResponse response,
            AiScheduleGenerateRequest request) {
        if (response.getWarnings() == null) {
            response.setWarnings(new ArrayList<>());
        }
        if (response.getOptimizationReasons() == null) {
            response.setOptimizationReasons(new ArrayList<>());
        }
        if (!StringUtils.hasText(response.getSummary())) {
            response.setSummary(
                    "Generated an AI schedule draft for administrator review.");
        }

        Set<LocalDate> unavailable = new HashSet<>(
                nullToEmpty(request.getUnavailableDates()));
        List<AiScheduleItem> normalized = nullToEmpty(response.getItems())
                .stream()
                .filter(Objects::nonNull)
                .map(item -> normalizeItem(item, request))
                .filter(item -> isValidGeneratedItem(
                        item, request, unavailable, response.getWarnings()))
                .sorted(Comparator.comparing(AiScheduleItem::getWorkDate)
                        .thenComparing(AiScheduleItem::getStartTime))
                .limit(MAX_GENERATED_ITEMS)
                .toList();
        response.setItems(new ArrayList<>(normalized));
        if (response.getItems().isEmpty()) {
            response.getWarnings().add(
                    "No valid schedule items were generated.");
        }
    }

    private AiScheduleItem normalizeItem(
            AiScheduleItem item,
            AiScheduleGenerateRequest request) {
        item.setDoctorId(request.getDoctorId());
        item.setDoctorName(request.getDoctorName());
        item.setDeptId(request.getDeptId());
        if (item.getMaxNum() == null || item.getMaxNum() <= 0) {
            item.setMaxNum(request.getDefaultMaxNum());
        }
        if (item.getPrice() == null) {
            item.setPrice(request.getDefaultPrice());
        }
        if (!StringUtils.hasText(item.getRoom())) {
            item.setRoom(firstRoom(request));
        }
        return item;
    }

    private boolean isValidGeneratedItem(
            AiScheduleItem item,
            AiScheduleGenerateRequest request,
            Set<LocalDate> unavailable,
            List<String> warnings) {
        if (item.getWorkDate() == null
                || item.getStartTime() == null
                || item.getEndTime() == null) {
            warnings.add("Ignored an item missing date or time.");
            return false;
        }
        if (item.getWorkDate().isBefore(request.getPeriodStart())
                || item.getWorkDate().isAfter(request.getPeriodEnd())) {
            warnings.add("Ignored item outside requested period: "
                    + item.getWorkDate());
            return false;
        }
        if (unavailable.contains(item.getWorkDate())) {
            warnings.add("Ignored unavailable date: " + item.getWorkDate());
            return false;
        }
        if (!item.getStartTime().isBefore(item.getEndTime())) {
            warnings.add("Ignored item with invalid time range: "
                    + item.getWorkDate());
            return false;
        }
        return true;
    }

    private AiScheduleGenerateResponse buildRuleBasedFallback(
            AiScheduleGenerateRequest request) {
        AiScheduleGenerateResponse response =
                new AiScheduleGenerateResponse();
        response.setSummary("Generated a basic schedule draft from configured windows.");
        response.setOptimizationReasons(List.of(
                "Uses administrator configured time windows.",
                "Skips unavailable dates.",
                "Conflicts are checked before publishing."));

        List<AiScheduleTimeWindow> windows = effectiveWindows(request);
        Set<LocalDate> unavailable = new HashSet<>(
                nullToEmpty(request.getUnavailableDates()));
        List<AiScheduleItem> items = new ArrayList<>();
        int roomIndex = 0;
        for (LocalDate date = request.getPeriodStart();
                !date.isAfter(request.getPeriodEnd());
                date = date.plusDays(1)) {
            if (unavailable.contains(date)) {
                continue;
            }
            for (AiScheduleTimeWindow window : windows) {
                if (!matchesDay(date, window.getDayOfWeek())) {
                    continue;
                }
                AiScheduleItem item = new AiScheduleItem();
                item.setDoctorId(request.getDoctorId());
                item.setDoctorName(request.getDoctorName());
                item.setDeptId(request.getDeptId());
                item.setWorkDate(date);
                item.setStartTime(window.getStartTime());
                item.setEndTime(window.getEndTime());
                item.setMaxNum(window.getMaxNum() == null
                        ? request.getDefaultMaxNum() : window.getMaxNum());
                item.setPrice(window.getPrice() == null
                        ? request.getDefaultPrice() : window.getPrice());
                item.setRoom(StringUtils.hasText(window.getRoom())
                        ? window.getRoom()
                        : nextRoom(request, roomIndex++));
                items.add(item);
                if (items.size() >= MAX_GENERATED_ITEMS) {
                    break;
                }
            }
            if (items.size() >= MAX_GENERATED_ITEMS) {
                break;
            }
        }
        response.setItems(items);
        return response;
    }

    private List<AiScheduleTimeWindow> effectiveWindows(
            AiScheduleGenerateRequest request) {
        List<AiScheduleTimeWindow> configured =
                nullToEmpty(request.getTimeWindows()).stream()
                        .filter(this::validWindow)
                        .toList();
        if (!configured.isEmpty()) {
            return configured;
        }

        List<AiScheduleTimeWindow> defaults = new ArrayList<>();
        for (int day = 1; day <= 5; day++) {
            defaults.add(defaultWindow(day, LocalTime.of(9, 0),
                    LocalTime.of(12, 0), request));
            defaults.add(defaultWindow(day, LocalTime.of(14, 0),
                    LocalTime.of(17, 0), request));
        }
        return defaults;
    }

    private AiScheduleTimeWindow defaultWindow(
            int dayOfWeek,
            LocalTime start,
            LocalTime end,
            AiScheduleGenerateRequest request) {
        AiScheduleTimeWindow window = new AiScheduleTimeWindow();
        window.setDayOfWeek(dayOfWeek);
        window.setStartTime(start);
        window.setEndTime(end);
        window.setMaxNum(request.getDefaultMaxNum());
        window.setPrice(request.getDefaultPrice());
        return window;
    }

    private boolean validWindow(AiScheduleTimeWindow window) {
        return window != null
                && window.getStartTime() != null
                && window.getEndTime() != null
                && window.getStartTime().isBefore(window.getEndTime());
    }

    private boolean matchesDay(LocalDate date, Integer configuredDayOfWeek) {
        if (configuredDayOfWeek == null) {
            return true;
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek.getValue() == configuredDayOfWeek;
    }

    private List<DoctorSchedule> fetchExistingSchedules(
            AiScheduleGenerateRequest request,
            String adminToken,
            List<String> warnings) {
        try {
            Result<List<DoctorSchedule>> result =
                    adminFeignClient.getDoctorSchedulesForAI(
                            adminToken,
                            request.getDoctorId(),
                            request.getPeriodStart().toString(),
                            request.getPeriodEnd().toString());
            if (ResultCode.SUCCESS.equals(result.getCode())) {
                return result.getData() == null ? List.of() : result.getData();
            }
            warnings.add("Failed to read existing schedules: "
                    + valueOrDefault(result.getMsg(), "admin-service error"));
        } catch (Exception exception) {
            warnings.add("Failed to read existing schedules: "
                    + exception.getClass().getSimpleName());
        }
        return List.of();
    }

    private void markConflicts(
            List<AiScheduleItem> items,
            String adminToken,
            List<String> warnings) {
        for (AiScheduleItem item : nullToEmpty(items)) {
            try {
                Result<Boolean> result =
                        adminFeignClient.checkConflict(
                                adminToken, toDoctorSchedule(item));
                if (ResultCode.SUCCESS.equals(result.getCode())) {
                    boolean conflict = Boolean.TRUE.equals(result.getData());
                    item.setConflict(conflict);
                    item.setConflictReason(conflict
                            ? "Doctor already has a schedule in this time range."
                            : null);
                } else {
                    item.setConflict(true);
                    item.setConflictReason("Conflict check failed.");
                    warnings.add("Conflict check failed: " + valueOrDefault(
                            result.getMsg(), "admin-service error"));
                }
            } catch (Exception exception) {
                item.setConflict(true);
                item.setConflictReason("Conflict check exception.");
                warnings.add("Conflict check exception: "
                        + exception.getClass().getSimpleName());
            }
        }
    }

    private DoctorSchedule toDoctorSchedule(AiScheduleItem item) {
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDoctorId(item.getDoctorId());
        schedule.setDoctorName(item.getDoctorName());
        schedule.setDeptId(item.getDeptId());
        schedule.setWorkDate(item.getWorkDate());
        schedule.setStartTime(item.getStartTime());
        schedule.setEndTime(item.getEndTime());
        schedule.setMaxNum(item.getMaxNum());
        schedule.setPrice(item.getPrice());
        schedule.setRoom(item.getRoom());
        return schedule;
    }

    private ScheduleSaveDto toScheduleSaveDto(AiScheduleItem item) {
        ScheduleSaveDto dto = new ScheduleSaveDto();
        dto.setDoctorId(item.getDoctorId());
        dto.setDoctorName(item.getDoctorName());
        dto.setDeptId(item.getDeptId());
        dto.setWorkDate(item.getWorkDate());
        dto.setStartTime(item.getStartTime());
        dto.setEndTime(item.getEndTime());
        dto.setMaxNum(item.getMaxNum());
        dto.setPrice(item.getPrice());
        dto.setRoom(item.getRoom());
        return dto;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private String firstRoom(AiScheduleGenerateRequest request) {
        return nullToEmpty(request.getRooms()).stream()
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("");
    }

    private String nextRoom(AiScheduleGenerateRequest request, int index) {
        List<String> rooms = nullToEmpty(request.getRooms()).stream()
                .filter(StringUtils::hasText)
                .toList();
        if (rooms.isEmpty()) {
            return "";
        }
        return rooms.get(Math.floorMod(index, rooms.size()));
    }

    private String valueOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private <T> List<T> nullToEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }
}

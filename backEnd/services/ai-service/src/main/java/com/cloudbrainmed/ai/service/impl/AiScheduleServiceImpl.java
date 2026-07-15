package com.cloudbrainmed.ai.service.impl;

import com.cloudbrainmed.admin.dto.ScheduleBatchCreateResponse;
import com.cloudbrainmed.admin.dto.ScheduleConflictResult;
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
import com.cloudbrainmed.payment.controller.PayController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
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
import java.util.regex.Pattern;

@Service
public class AiScheduleServiceImpl implements AiScheduleService {

    private static final Logger log = LoggerFactory.getLogger(AiScheduleServiceImpl.class);

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
        this.chatClient = chatClientBuilder
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(modelName)
                        .temperature(0.1)
                        .maxTokens(4096)
                        .build())
                .build();
        this.objectMapper = objectMapper;
        this.adminFeignClient = adminFeignClient;
        this.modelName = modelName;
    }

    @Override
    public AiScheduleGenerateResponse preview(
            AiScheduleGenerateRequest request, String adminId) {
        List<String> warnings = new ArrayList<>();

        // 添加日志：记录原始需求
        String requirement = valueOrDefault(request.getRequirement(), "");
        log.info("AI排班预览 - 原始需求: {}", requirement);

        List<DoctorSchedule> existingSchedules =
                fetchExistingSchedules(request, warnings);
        List<DoctorSchedule> roomUsages =
                fetchRoomUsages(request, warnings);

        AiScheduleGenerateResponse response;
        try {
            String reply = chatClient.prompt(new Prompt(buildMessages(
                    request, existingSchedules, roomUsages))).call().content();
            log.info("AI排班预览 - AI回复长度: {}", reply.length());
            response = parseReply(reply);
            response.setFallback(false);
            response.setStatus("SUCCESS");
        } catch (Exception exception) {
            log.error("AI排班预览失败，使用降级方案", exception);
            response = buildRuleBasedFallback(request);
            response.setFallback(true);
            response.setStatus("FALLBACK");
            warnings.add("AI model is unavailable; generated a rule-based schedule draft.");
            warnings.add("Error type: " + exception.getClass().getSimpleName());
        }
        normalizeResponse(response, request);
        response.getWarnings().addAll(0, warnings);
        response.setModelVersion(modelName);
        markConflicts(response.getItems(), response.getWarnings());
        return response;
    }

    @Override
    public AiScheduleGenerateResponse checkConflicts(
            AiScheduleConflictCheckRequest request) {
        AiScheduleGenerateResponse response = new AiScheduleGenerateResponse();
        response.setStatus("SUCCESS");
        response.setModelVersion(modelName);
        response.setSummary("Conflict check completed.");
        response.setItems(new ArrayList<>(nullToEmpty(request.getItems())));
        markConflicts(response.getItems(), response.getWarnings());
        return response;
    }

    @Override
    public AiSchedulePublishResponse publish(
            AiSchedulePublishRequest request, String adminId) {
        AiSchedulePublishResponse response = new AiSchedulePublishResponse();
        response.setTraceId(request.getTraceId());
        List<AiScheduleItem> items = new ArrayList<>(
                nullToEmpty(request.getItems()));
        response.setSubmittedCount(items.size());

        markConflicts(items, response.getWarnings());
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

        Result<ScheduleBatchCreateResponse> result =
                adminFeignClient.batchCreateSchedules(publishable);
        if (!ResultCode.SUCCESS.equals(result.getCode())) {
            response.setStatus("FAILED");
            response.getWarnings().add(valueOrDefault(
                    result.getMsg(), "admin-service failed to create schedules."));
            return response;
        }

        ScheduleBatchCreateResponse batchResponse = result.getData();
        List<DoctorSchedule> created = batchResponse == null
                ? List.of() : nullToEmpty(batchResponse.getCreatedSchedules());
        response.setCreatedSchedules(created);
        response.setCreatedCount(created.size());
        if (batchResponse != null) {
            response.setFailedItems(new ArrayList<>(
                    nullToEmpty(batchResponse.getFailedItems())));
            response.getWarnings().addAll(nullToEmpty(batchResponse.getWarnings()));
        }
        response.setStatus(created.isEmpty()
                ? "FAILED"
                : created.size() == publishable.size()
                ? "SUCCESS" : "PARTIAL_SUCCESS");
        if (created.size() < publishable.size()) {
            response.getWarnings().add(
                    "Some schedules were not created by admin-service validation.");
        }
        return response;
    }

    // ================================================================
    // 需求解析
    // ================================================================

    /**
     * 排班需求解析结果
     */
    private static class ScheduleRequirement {
        // 周末规则
        boolean weekendOff = false;      // 周六周日都休息
        boolean weekendOn = false;       // 周末也上班
        boolean saturdayOff = false;     // 只周六休息
        boolean saturdayOn = false;      // 周六上班（用于"周二四六"等场景）
        boolean sundayOff = false;       // 只周日休息
        boolean noDayOff = false;        // 无休

        // 时段模式
        String shiftMode = "";

        // 特定日期
        boolean mondayOnly = false;
        boolean tuesdayOnly = false;
        boolean wednesdayOnly = false;
        boolean thursdayOnly = false;
        boolean fridayOnly = false;

        // 优先级
        String priority = "";

        // 数量限制
        int maxWorkingDays = 0;
        int maxSchedules = 0;
    }

    /**
     * 解析需求文本，识别排班模式
     */
    private ScheduleRequirement parseRequirement(String requirement) {
        ScheduleRequirement req = new ScheduleRequirement();
        if (!StringUtils.hasText(requirement)) {
            return req;
        }

        String text = requirement.toLowerCase().trim();

        // ===== 1. 工作日/周末规则 =====
        if (containsAny(text, "周六周日休息", "周末休息", "周六日休息", "双休",
                "周六周日不上班", "周六日不上班", "每周末休息", "周末都休息",
                "周六日都休息", "周末全休")) {
            req.weekendOff = true;
        } else if (containsAny(text, "周六上班", "周日上班", "周末上班", "周末也上班",
                "包括周末", "含周末", "周末也排", "周末也要", "周六周日上班",
                "周末不休息", "周末正常上班")) {
            req.weekendOn = true;
        } else if (containsAny(text, "周六休息", "周六不上班", "周六不排班")) {
            req.saturdayOff = true;
        } else if (containsAny(text, "周日休息", "周日不上班", "周日不排班")) {
            req.sundayOff = true;
        } else if (containsAny(text, "无休", "不休息", "每天上班", "天天上班", "全年无休", "全周无休")) {
            req.noDayOff = true;
        }

        // ===== 2. 时段规则 =====

        // 交替模式
        if (containsAny(text, "一天上午值班一天下午值班", "一天上午一天下午",
                "上午下午交替", "上下午交替", "上午下午轮换")) {
            req.shiftMode = "ALTERNATE_AM_PM";
        } else if (containsAny(text, "一天早班一天晚班", "早班晚班交替",
                "早晚班交替", "早班晚班轮换")) {
            req.shiftMode = "ALTERNATE_MORNING_NIGHT";
        } else if (containsAny(text, "一天白班一天夜班", "白班夜班交替", "白夜班交替")) {
            req.shiftMode = "ALTERNATE_DAY_NIGHT";
        }

        // 单一时段模式 - 增强匹配
        else if (containsAny(text, "只排上午", "只上上午", "仅上午", "只排上午的班",
                "只要上午", "仅排上午", "上午半天", "上午班", "排上午",
                "上午值班", "上午出诊", "上午坐诊", "只有上午", "仅有上午",
                "上午门诊", "只上午")) {
            req.shiftMode = "MORNING_ONLY";
        } else if (containsAny(text, "只排下午", "只上下午", "仅下午", "只排下午的班",
                "只要下午", "仅排下午", "下午半天", "下午班", "排下午",
                "下午值班", "下午出诊", "下午坐诊", "只有下午", "仅有下午",
                "下午门诊", "只下午")) {
            req.shiftMode = "AFTERNOON_ONLY";
        } else if (containsAny(text, "只排晚上", "只上晚上", "仅晚上", "只排晚上的班",
                "只排夜班", "只上夜班", "仅夜班", "只要晚上", "只要夜班",
                "只有晚上", "仅有晚上", "晚上门诊", "夜班门诊", "只晚上",
                "夜班出诊", "晚上出诊")) {
            req.shiftMode = "NIGHT_ONLY";
        } else if (containsAny(text, "只排早班", "只上早班", "仅早班", "早班门诊")) {
            req.shiftMode = "MORNING_SHIFT_ONLY";
        } else if (containsAny(text, "只排白班", "只上白班", "仅白班", "白班门诊")) {
            req.shiftMode = "DAY_SHIFT_ONLY";
        }

        // 混合模式
        else if (containsAny(text, "上午和下午", "上下午都排", "上下午都要",
                "上午下午都排", "全天", "整天", "全天班", "全天出诊", "全天坐诊",
                "全天值班", "一天全天", "全日出诊", "全日门诊", "全天门诊",
                "上午下午都要")) {
            req.shiftMode = "AM_AND_PM";
        } else if (containsAny(text, "上午和晚上", "早班和夜班")) {
            req.shiftMode = "AM_AND_NIGHT";
        } else if (containsAny(text, "下午和晚上", "下午和夜班")) {
            req.shiftMode = "PM_AND_NIGHT";
        } else if (containsAny(text, "上午下午晚上", "三班", "三个时段", "早中晚", "三班倒", "全时段")) {
            req.shiftMode = "ALL_SHIFTS";
        }

        // ===== 3. 特定日期规则 =====
        Pattern monPattern = Pattern.compile("只排周[一1]|仅周[一1]|只在周[一1]|每周一|周一门诊|周一坐诊");
        Pattern tuePattern = Pattern.compile("只排周[二2]|仅周[二2]|只在周[二2]|每周二|周二门诊|周二坐诊");
        Pattern wedPattern = Pattern.compile("只排周[三3]|仅周[三3]|只在周[三3]|每周三|周三门诊|周三坐诊");
        Pattern thuPattern = Pattern.compile("只排周[四4]|仅周[四4]|只在周[四4]|每周四|周四门诊|周四坐诊");
        Pattern friPattern = Pattern.compile("只排周[五5]|仅周[五5]|只在周[五5]|每周五|周五门诊|周五坐诊");

        if (monPattern.matcher(text).find()) req.mondayOnly = true;
        if (tuePattern.matcher(text).find()) req.tuesdayOnly = true;
        if (wedPattern.matcher(text).find()) req.wednesdayOnly = true;
        if (thuPattern.matcher(text).find()) req.thursdayOnly = true;
        if (friPattern.matcher(text).find()) req.fridayOnly = true;

        // 特定组合
        if (containsAny(text, "每周一三五", "周一三五", "周一、三、五", "一三五")) {
            req.mondayOnly = true;
            req.wednesdayOnly = true;
            req.fridayOnly = true;
        }
        if (containsAny(text, "每周二四六", "周二四六", "周二、四、六", "二四六")) {
            req.tuesdayOnly = true;
            req.thursdayOnly = true;
            req.saturdayOn = true;
        }

        // ===== 4. 数量限制 =====
        Pattern numPattern = Pattern.compile("排(\\d+)天|只排(\\d+)天|最多(\\d+)天|排(\\d+)天班");
        java.util.regex.Matcher numMatcher = numPattern.matcher(text);
        if (numMatcher.find()) {
            try {
                for (int i = 1; i <= numMatcher.groupCount(); i++) {
                    if (numMatcher.group(i) != null) {
                        req.maxWorkingDays = Integer.parseInt(numMatcher.group(i));
                        break;
                    }
                }
            } catch (NumberFormatException ignored) {}
        }

        Pattern countPattern = Pattern.compile("(\\d+)[个条次]排班|生成(\\d+)条|排(\\d+)条|(\\d+)条记录");
        java.util.regex.Matcher countMatcher = countPattern.matcher(text);
        if (countMatcher.find()) {
            try {
                for (int i = 1; i <= countMatcher.groupCount(); i++) {
                    if (countMatcher.group(i) != null) {
                        req.maxSchedules = Integer.parseInt(countMatcher.group(i));
                        break;
                    }
                }
            } catch (NumberFormatException ignored) {}
        }

        // ===== 5. 优先级规则 - 增强匹配 =====
        if (containsAny(text, "优先上午", "上午优先", "尽量上午", "尽可能上午",
                "上午为主", "主要上午", "优先排上午")) {
            req.priority = "AM_PRIORITY";
        } else if (containsAny(text, "优先下午", "下午优先", "尽量下午", "尽可能下午",
                "下午为主", "主要下午", "优先排下午")) {
            req.priority = "PM_PRIORITY";
        } else if (containsAny(text, "优先晚上", "晚上优先", "尽量晚上", "优先夜班", "尽可能晚上",
                "晚上为主", "主要晚上", "优先排晚上", "夜班优先")) {
            req.priority = "NIGHT_PRIORITY";
        } else if (containsAny(text, "优先工作日", "工作日优先", "尽量工作日", "工作日为主")) {
            req.priority = "WEEKDAY_PRIORITY";
        }

        return req;
    }

    /**
     * 判断文本是否包含任意一个关键词
     */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    // ================================================================
    // 构建 Prompt
    // ================================================================

    private List<Message> buildMessages(
            AiScheduleGenerateRequest request,
            List<DoctorSchedule> existingSchedules,
            List<DoctorSchedule> roomUsages) {

        // 直接使用原始需求，不再解析
        String systemPrompt = buildSimpleSystemPrompt();
        String userPrompt = buildUserPrompt(request, existingSchedules, roomUsages);

        return List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt));
    }

    /**
     * 简化的 System Prompt，让AI自己理解需求
     */
    private String buildSimpleSystemPrompt() {
        return """
        You are an AI scheduling assistant for a hospital administrator.
        Generate a doctor schedule draft based on the administrator's requirements.
        
        ╔══════════════════════════════════════════════════════════════╗
        ║              CORE RULES (MUST FOLLOW)                        ║
        ╚══════════════════════════════════════════════════════════════╝
        
        1. DOCTOR INFO: Use ONLY the provided doctorId, doctorName, deptId.
        2. DATE RANGE: Generate schedules within periodStart to periodEnd (inclusive).
        3. UNAVAILABLE DATES: Absolutely skip dates in unavailableDates list.
        4. EXISTING SCHEDULES: Avoid time conflicts with existingSchedules.
        5. ROOM CONFLICTS: Avoid rooms in roomUsages for the same date+time.
        6. TIME VALIDITY: startTime must be < endTime.
        7. QUOTA & PRICE: Use defaultMaxNum and defaultPrice unless timeWindow says otherwise.
        
        ╔══════════════════════════════════════════════════════════════╗
        ║           UNDERSTAND THE ADMIN'S REQUIREMENT                 ║
        ╚══════════════════════════════════════════════════════════════╝
        
        The admin has provided a natural language requirement.
        You MUST carefully interpret it and apply ALL rules mentioned.
        
        Common requirement patterns and how to handle them:
        
        【休息/不排班】
        - "周六休息" / "周日休息" → Skip that day entirely
        - "周末休息" / "双休" → Skip Saturday AND Sunday
        - "周六周日都休息" → Skip both days
        - "周末也上班" → Include Saturday and Sunday
        
        【时段限制】
        - "只排上午" / "仅上午" / "上午门诊" → ONLY morning shifts (startTime < 12:00)
        - "只排下午" / "仅下午" / "下午门诊" → ONLY afternoon shifts (12:00 ≤ startTime < 18:00)
        - "只排晚上" / "夜班" → ONLY night shifts (startTime ≥ 18:00)
        - "全天" / "上午下午都排" → BOTH morning and afternoon
        - "三班倒" / "全时段" → ALL configured timeWindows
        
        【交替排班】
        - "一天上午一天下午" / "上午下午交替" → ALTERNATE: Day 1 AM, Day 2 PM, Day 3 AM...
        - "早班晚班交替" → ALTERNATE: Day 1 morning, Day 2 night, Day 3 morning...
        - "白班夜班交替" → ALTERNATE: Day 1 day(am+pm), Day 2 night, Day 3 day...
        
        【特定日期规则】
        - "周一上午，周三下午" → Different rules for different days
        - "周一到周五全天，周六上午" → Weekdays full day, Saturday morning only
        - "周日只排下午" → Sunday afternoon only
        
        【优先级】
        - "优先上午" / "尽量上午" → Prefer morning shifts, but use others if needed
        - "优先下午" → Prefer afternoon shifts
        
        【数量限制】
        - "只排3天" → Maximum 3 working days
        - "排5条" → Maximum 5 schedule items
        
        IMPORTANT: If the requirement has MULTIPLE parts separated by commas or other punctuation,
        treat EACH part as a separate rule that ALL must be satisfied.
        
        For example: "周六休息，周日下午门诊，周一到周五一天上午一天下午门诊"
        → Part 1: Saturday = OFF (no schedules)
        → Part 2: Sunday = AFTERNOON ONLY
        → Part 3: Monday-Friday = ALTERNATING AM/PM
        
        ╔══════════════════════════════════════════════════════════════╗
        ║                    OUTPUT FORMAT                             ║
        ╚══════════════════════════════════════════════════════════════╝
        
        Return valid JSON ONLY, no markdown, no code blocks.
        
        {
          "summary": "schedule strategy summary in Chinese, explain how you understood the requirement",
          "items": [{
            "doctorId": "doctor id",
            "doctorName": "doctor name", 
            "deptId": "department id",
            "workDate": "yyyy-MM-dd",
            "startTime": "HH:mm:ss",
            "endTime": "HH:mm:ss",
            "maxNum": 30,
            "price": 0.00,
            "room": "room name"
          }],
          "warnings": ["any issues or notes"],
          "optimizationReasons": ["why this schedule pattern was chosen based on the requirement"]
        }
        """;
    }

    private String buildUserPrompt(
            AiScheduleGenerateRequest request,
            List<DoctorSchedule> existingSchedules,
            List<DoctorSchedule> roomUsages) {

        return String.format("""
        ╔══════════════════════════════════════════════════════════════╗
        ║                  SCHEDULE REQUEST DATA                       ║
        ╚══════════════════════════════════════════════════════════════╝
        
        doctorId: %s
        doctorName: %s
        deptId: %s
        periodStart: %s
        periodEnd: %s
        
        ═══════════════════════════════════════════════════════════════
        ADMINISTRATOR'S REQUIREMENT (Natural Language):
        ═══════════════════════════════════════════════════════════════
        %s
        ═══════════════════════════════════════════════════════════════
        
        CONFIGURED PARAMETERS:
        - defaultMaxNum: %s (max patients per schedule)
        - defaultPrice: %s (price per schedule)
        
        - rooms: %s
        - timeWindows (configured time slots): %s
        - unavailableDates: %s
        
        EXISTING DATA (to avoid conflicts):
        - existingSchedules: %s
        - roomUsages (already occupied): %s
        
        ╔══════════════════════════════════════════════════════════════╗
        ║                      YOUR TASK                              ║
        ╚══════════════════════════════════════════════════════════════╝
        
        Read the ADMINISTRATOR'S REQUIREMENT carefully.
        Understand what they want, then generate the schedule.
        
        KEY POINTS:
        1. Parse ALL parts of the requirement (split by commas/periods if needed)
        2. Apply all rules simultaneously
        3. Use the configured timeWindows to determine available time slots
        4. Skip unavailableDates
        5. Avoid conflicts with existingSchedules and roomUsages
        
        Return ONLY valid JSON, no markdown, no extra text.
        """,
                request.getDoctorId(),
                request.getDoctorName(),
                request.getDeptId(),
                request.getPeriodStart(),
                request.getPeriodEnd(),
                valueOrDefault(request.getRequirement(), "Generate a standard Monday-Friday schedule with all available time slots"),
                request.getDefaultMaxNum(),
                request.getDefaultPrice(),
                toJson(request.getRooms()),
                toJson(effectiveWindows(request)),
                toJson(request.getUnavailableDates()),
                toJson(existingSchedules),
                toJson(roomUsages)
        );
    }

    private String buildSystemPrompt(ScheduleRequirement req) {
        StringBuilder sb = new StringBuilder();

        sb.append("""
            You are an AI scheduling assistant for a hospital administrator.
            Generate a doctor schedule draft that can be reviewed before publishing.
            
            ╔══════════════════════════════════════════════════════════════╗
            ║              CRITICAL RULES (MUST FOLLOW STRICTLY)           ║
            ╚══════════════════════════════════════════════════════════════╝
            
            【BASIC RULES - 基本规则】
            1. DOCTOR: Use ONLY the provided doctorId, doctorName, deptId.
               Do NOT invent any doctor or department.
            2. DATE RANGE: Only generate schedules within periodStart to periodEnd (inclusive).
            3. UNAVAILABLE DATES: Absolutely skip all dates listed in unavailableDates.
            4. EXISTING SCHEDULES: Avoid time conflicts with existingSchedules.
            5. ROOM CONFLICTS: Avoid rooms listed in roomUsages for same date+time.
            6. TIME VALIDITY: Every schedule must have startTime < endTime.
            7. QUOTA & PRICE: Use defaultMaxNum and defaultPrice unless timeWindow overrides.
            
            【TIME CLASSIFICATION - 时段分类】
            - MORNING (上午): startTime < 12:00
            - AFTERNOON (下午): 12:00 ≤ startTime < 18:00
            - NIGHT (晚上/夜班): startTime ≥ 18:00
            
            【DAY MAPPING - 星期映射】
            Monday=1, Tuesday=2, Wednesday=3, Thursday=4, Friday=5, Saturday=6, Sunday=7
            
            """);

        // ===== 周末/休息规则 =====
        sb.append("""
            ┌─────────────────────────────────────────────────────────────┐
            │              WORKDAY/WEEKEND RULES - 工作日规则             │
            └─────────────────────────────────────────────────────────────┘
            
            """);

        if (req.weekendOff) {
            sb.append("""
                ⚠️  CURRENT RULE: WEEKEND OFF (周六周日休息/双休)
                → ONLY generate schedules for Monday(1) through Friday(5).
                → COMPLETELY SKIP Saturday(6) and Sunday(7).
                → DO NOT generate any schedule for Saturday or Sunday.
                
                """);
        } else if (req.saturdayOff && req.sundayOff) {
            sb.append("""
                ⚠️  CURRENT RULE: BOTH SATURDAY AND SUNDAY OFF
                → ONLY generate for Monday(1) through Friday(5).
                
                """);
        } else if (req.saturdayOff) {
            sb.append("""
                ⚠️  CURRENT RULE: SATURDAY OFF (周六休息)
                → Skip Saturday(6), but KEEP Sunday(7) if within date range.
                
                """);
        } else if (req.sundayOff) {
            sb.append("""
                ⚠️  CURRENT RULE: SUNDAY OFF (周日休息)
                → Skip Sunday(7), but KEEP Saturday(6) if within date range.
                
                """);
        } else if (req.weekendOn) {
            sb.append("""
                ⚠️  CURRENT RULE: WEEKEND ON (周末也上班)
                → Include Saturday(6) and Sunday(7) in the schedule.
                → Generate for ALL 7 days of the week.
                
                """);
        } else if (req.noDayOff) {
            sb.append("""
                ⚠️  CURRENT RULE: NO DAY OFF (无休/每天上班)
                → Generate schedules for ALL 7 days of the week.
                → Do NOT skip any day.
                
                """);
        } else if (req.saturdayOn) {
            sb.append("""
                ⚠️  CURRENT RULE: SATURDAY INCLUDED (周六上班)
                → Saturday(6) is a working day. Include it in the schedule.
                
                """);
        } else {
            sb.append("""
                ⚠️  DEFAULT: Standard work week (Monday-Friday)
                → By default, only generate for Monday through Friday.
                → Skip Saturday(6) and Sunday(7) unless explicitly required.
                
                """);
        }

        // ===== 特定日期规则 =====
        if (req.mondayOnly || req.tuesdayOnly || req.wednesdayOnly ||
                req.thursdayOnly || req.fridayOnly || req.saturdayOn) {
            sb.append("    【SPECIFIC DAYS - 特定日期要求】\n");
            sb.append("    → ONLY generate schedules for: ");
            List<String> days = new ArrayList<>();
            if (req.mondayOnly) days.add("Monday");
            if (req.tuesdayOnly) days.add("Tuesday");
            if (req.wednesdayOnly) days.add("Wednesday");
            if (req.thursdayOnly) days.add("Thursday");
            if (req.fridayOnly) days.add("Friday");
            if (req.saturdayOn) days.add("Saturday");
            sb.append(String.join(", ", days));
            sb.append("\n    → SKIP all other days completely.\n\n");
        }

        // ===== 时段规则 =====
        sb.append("""
            ┌─────────────────────────────────────────────────────────────┐
            │              SHIFT MODE RULES - 排班时段规则                │
            └─────────────────────────────────────────────────────────────┘
            
            """);

        switch (req.shiftMode) {
            case "ALTERNATE_AM_PM":
                sb.append("""
                    ⚠️  CURRENT MODE: ALTERNATING AM/PM (上午下午交替)
                    
                    RULES:
                    → Count ONLY working days (after applying weekend/day-off rules).
                    → Working Day 1 (first working day): MORNING ONLY (only timeWindows with startTime < 12:00)
                    → Working Day 2 (second working day): AFTERNOON ONLY (only timeWindows with 12:00 ≤ startTime < 18:00)
                    → Working Day 3: MORNING ONLY
                    → Working Day 4: AFTERNOON ONLY
                    → Continue this alternating pattern for ALL working days.
                    → Each working day gets EXACTLY ONE shift (either AM or PM, never both).
                    
                    EXAMPLE (Mon-Fri with weekend off):
                    Mon(working day 1): 08:00-12:00 ← AM only
                    Tue(working day 2): 14:00-18:00 ← PM only
                    Wed(working day 3): 08:00-12:00 ← AM only
                    Thu(working day 4): 14:00-18:00 ← PM only
                    Fri(working day 5): 08:00-12:00 ← AM only
                    
                    """);
                break;

            case "ALTERNATE_MORNING_NIGHT":
                sb.append("""
                    ⚠️  CURRENT MODE: ALTERNATING MORNING/NIGHT (早班晚班交替)
                    
                    RULES:
                    → Working Day 1: MORNING ONLY (startTime < 12:00)
                    → Working Day 2: NIGHT ONLY (startTime ≥ 18:00)
                    → Working Day 3: MORNING ONLY
                    → Working Day 4: NIGHT ONLY
                    → Continue alternating.
                    
                    """);
                break;

            case "ALTERNATE_DAY_NIGHT":
                sb.append("""
                    ⚠️  CURRENT MODE: ALTERNATING DAY/NIGHT (白班夜班交替)
                    
                    RULES:
                    → Working Day 1: DAY SHIFT (startTime < 18:00, use available morning+afternoon windows)
                    → Working Day 2: NIGHT SHIFT (startTime ≥ 18:00)
                    → Continue alternating.
                    
                    """);
                break;

            case "MORNING_ONLY":
                sb.append("""
                    ⚠️  CURRENT MODE: MORNING ONLY (只排上午)
                    
                    RULES:
                    → ALL working days: ONLY morning shifts (startTime < 12:00)
                    → DO NOT generate any afternoon or night shifts.
                    → If multiple morning timeWindows exist, use all of them for each day.
                    
                    """);
                break;

            case "AFTERNOON_ONLY":
                sb.append("""
                    ⚠️  CURRENT MODE: AFTERNOON ONLY (只排下午)
                    
                    RULES:
                    → ALL working days: ONLY afternoon shifts (12:00 ≤ startTime < 18:00)
                    → DO NOT generate any morning or night shifts.
                    
                    """);
                break;

            case "NIGHT_ONLY":
                sb.append("""
                    ⚠️  CURRENT MODE: NIGHT ONLY (只排晚上/夜班)
                    
                    RULES:
                    → ALL working days: ONLY night shifts (startTime ≥ 18:00)
                    → DO NOT generate any morning or afternoon shifts.
                    → If no night timeWindow exists in the configured list, use the latest available window.
                    
                    """);
                break;

            case "MORNING_SHIFT_ONLY":
                sb.append("""
                    ⚠️  CURRENT MODE: EARLY MORNING ONLY (只排早班)
                    
                    RULES:
                    → ALL working days: ONLY the earliest available time window.
                    → Typically the first configured morning shift.
                    
                    """);
                break;

            case "DAY_SHIFT_ONLY":
                sb.append("""
                    ⚠️  CURRENT MODE: DAY SHIFT ONLY (只排白班)
                    
                    RULES:
                    → ALL working days: ALL day shifts (startTime < 18:00).
                    → Include both morning and afternoon timeWindows for each day.
                    
                    """);
                break;

            case "AM_AND_PM":
                sb.append("""
                    ⚠️  CURRENT MODE: BOTH AM AND PM (上午和下午都排/全天班)
                    
                    RULES:
                    → ALL working days: BOTH morning AND afternoon shifts.
                    → Each day gets ALL morning timeWindows + ALL afternoon timeWindows.
                    
                    """);
                break;

            case "AM_AND_NIGHT":
                sb.append("""
                    ⚠️  CURRENT MODE: AM AND NIGHT (上午和晚上)
                    
                    RULES:
                    → ALL working days: morning shifts + night shifts.
                    → Each day gets morning timeWindows + night timeWindows.
                    
                    """);
                break;

            case "PM_AND_NIGHT":
                sb.append("""
                    ⚠️  CURRENT MODE: PM AND NIGHT (下午和晚上)
                    
                    RULES:
                    → ALL working days: afternoon shifts + night shifts.
                    → Each day gets afternoon timeWindows + night timeWindows.
                    
                    """);
                break;

            case "ALL_SHIFTS":
                sb.append("""
                    ⚠️  CURRENT MODE: ALL SHIFTS (三班/全时段)
                    
                    RULES:
                    → ALL working days: ALL configured timeWindows.
                    → Use every single timeWindow for each working day.
                    
                    """);
                break;

            default:
                sb.append("""
                    ⚠️  DEFAULT MODE: Use all configured timeWindows
                    → If no specific shift rule is detected, generate all available time slots for each working day.
                    
                    """);
                break;
        }

        // ===== 优先级 =====
        if (StringUtils.hasText(req.priority)) {
            sb.append("    【PRIORITY - 优先级】\n");
            switch (req.priority) {
                case "AM_PRIORITY":
                    sb.append("    → When possible, prefer/fill morning shifts first.\n\n");
                    break;
                case "PM_PRIORITY":
                    sb.append("    → When possible, prefer/fill afternoon shifts first.\n\n");
                    break;
                case "NIGHT_PRIORITY":
                    sb.append("    → When possible, prefer/fill night shifts first.\n\n");
                    break;
                case "WEEKDAY_PRIORITY":
                    sb.append("    → Prioritize weekday scheduling, minimize weekend shifts.\n\n");
                    break;
            }
        }

        // ===== 数量限制 =====
        if (req.maxWorkingDays > 0) {
            sb.append(String.format("""
                【QUANTITY LIMIT - 数量限制】
                → Maximum working days to schedule: %d
                → Only schedule the first %d working days in the date range.
                
                """, req.maxWorkingDays, req.maxWorkingDays));
        }

        if (req.maxSchedules > 0) {
            sb.append(String.format("""
                → Maximum total schedule items: %d
                → Do NOT generate more than %d schedule entries total.
                
                """, req.maxSchedules, req.maxSchedules));
        }

        // ===== 输出格式 =====
        sb.append("""
            ┌─────────────────────────────────────────────────────────────┐
            │                    OUTPUT FORMAT                            │
            └─────────────────────────────────────────────────────────────┘
            
            Return valid JSON ONLY, without any markdown code blocks (no ```json, no ```).
            The response must be pure JSON starting with { and ending with }.
            
            {
              "summary": "schedule strategy summary in Chinese",
              "items": [{
                "doctorId": "doctor id",
                "doctorName": "doctor name",
                "deptId": "department id",
                "workDate": "yyyy-MM-dd",
                "startTime": "HH:mm:ss",
                "endTime": "HH:mm:ss",
                "maxNum": 30,
                "price": 0.00,
                "room": "room name"
              }],
              "warnings": ["notes for administrator"],
              "optimizationReasons": ["why this schedule pattern was chosen"]
            }
            
            IMPORTANT REMINDERS:
            1. Double-check weekend exclusion if specified.
            2. Verify the alternating pattern count starts from working day 1.
            3. Each working day in ALTERNATE mode gets EXACTLY ONE shift.
            4. Check that unavailableDates are all skipped.
            """);

        return sb.toString();
    }

    private String buildUserPrompt(
            AiScheduleGenerateRequest request,
            List<DoctorSchedule> existingSchedules,
            List<DoctorSchedule> roomUsages,
            ScheduleRequirement req) {

        return String.format("""
            ╔══════════════════════════════════════════════════════════════╗
            ║                  SCHEDULE REQUEST DATA                       ║
            ╚══════════════════════════════════════════════════════════════╝
            
            doctorId: %s
            doctorName: %s
            deptId: %s
            periodStart: %s
            periodEnd: %s
            
            adminRequirement (original): %s
            
            defaultMaxNum: %s
            defaultPrice: %s
            
            rooms: %s
            timeWindows: %s
            unavailableDates: %s
            
            existingSchedules (to avoid conflicts): %s
            roomUsages (already occupied): %s
            
            ╔══════════════════════════════════════════════════════════════╗
            ║                  PARSED REQUIREMENTS                        ║
            ╚══════════════════════════════════════════════════════════════╝
            
            Detected Shift Mode: %s
            Weekend Handling: %s
            Specific Days Only: %s
            Priority: %s
            Max Working Days: %s
            Max Total Schedules: %s
            
            ╔══════════════════════════════════════════════════════════════╗
            ║                      YOUR TASK                              ║
            ╚══════════════════════════════════════════════════════════════╝
            
            Generate the schedule following ALL rules in the system prompt.
            Return ONLY valid JSON, no markdown, no extra text.
            """,
                request.getDoctorId(),
                request.getDoctorName(),
                request.getDeptId(),
                request.getPeriodStart(),
                request.getPeriodEnd(),
                valueOrDefault(request.getRequirement(), "No special requirements"),
                request.getDefaultMaxNum(),
                request.getDefaultPrice(),
                toJson(request.getRooms()),
                toJson(effectiveWindows(request)),
                toJson(request.getUnavailableDates()),
                toJson(existingSchedules),
                toJson(roomUsages),
                StringUtils.hasText(req.shiftMode) ? req.shiftMode : "DEFAULT (use all timeWindows)",
                buildWeekendText(req),
                buildSpecificDaysText(req),
                StringUtils.hasText(req.priority) ? req.priority : "None",
                req.maxWorkingDays > 0 ? String.valueOf(req.maxWorkingDays) : "No limit",
                req.maxSchedules > 0 ? String.valueOf(req.maxSchedules) : "No limit"
        );
    }

    private String buildWeekendText(ScheduleRequirement req) {
        if (req.weekendOff) return "WEEKEND OFF - Skip Saturday & Sunday";
        if (req.saturdayOff) return "Saturday OFF, Sunday ON";
        if (req.sundayOff) return "Sunday OFF, Saturday ON";
        if (req.weekendOn) return "WEEKEND ON - Include Saturday & Sunday";
        if (req.noDayOff) return "NO DAYS OFF - All 7 days";
        if (req.saturdayOn) return "Saturday is a working day";
        return "Default (Monday-Friday only)";
    }

    private String buildSpecificDaysText(ScheduleRequirement req) {
        List<String> days = new ArrayList<>();
        if (req.mondayOnly) days.add("Monday");
        if (req.tuesdayOnly) days.add("Tuesday");
        if (req.wednesdayOnly) days.add("Wednesday");
        if (req.thursdayOnly) days.add("Thursday");
        if (req.fridayOnly) days.add("Friday");
        if (req.saturdayOn) days.add("Saturday");
        return days.isEmpty() ? "All allowed weekdays" : "Only: " + String.join(", ", days);
    }

    // ================================================================
    // 其余方法保持不变
    // ================================================================

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
        AiScheduleGenerateResponse response = new AiScheduleGenerateResponse();

        String requirement = valueOrDefault(request.getRequirement(), "");

        response.setSummary("AI service unavailable, generated a basic schedule draft (fallback mode).");
        response.setOptimizationReasons(List.of(
                "降级方案：使用规则引擎生成基础排班",
                "原始需求: " + (requirement.isEmpty() ? "无" : requirement)
        ));

        List<AiScheduleTimeWindow> windows = effectiveWindows(request);
        Set<LocalDate> unavailable = new HashSet<>(nullToEmpty(request.getUnavailableDates()));
        List<AiScheduleItem> items = new ArrayList<>();
        int roomIndex = 0;

        // Fallback 使用简单的规则：所有工作日 + 所有时段
        for (LocalDate date = request.getPeriodStart();
             !date.isAfter(request.getPeriodEnd());
             date = date.plusDays(1)) {

            if (unavailable.contains(date)) continue;

            DayOfWeek dow = date.getDayOfWeek();
            // 默认周一至周五
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) continue;

            for (AiScheduleTimeWindow window : windows) {
                if (items.size() >= MAX_GENERATED_ITEMS) break;

                AiScheduleItem item = new AiScheduleItem();
                item.setDoctorId(request.getDoctorId());
                item.setDoctorName(request.getDoctorName());
                item.setDeptId(request.getDeptId());
                item.setWorkDate(date);
                item.setStartTime(window.getStartTime());
                item.setEndTime(window.getEndTime());
                item.setMaxNum(window.getMaxNum() == null ? request.getDefaultMaxNum() : window.getMaxNum());
                item.setPrice(window.getPrice() == null ? request.getDefaultPrice() : window.getPrice());
                item.setRoom(StringUtils.hasText(window.getRoom()) ? window.getRoom() : nextRoom(request, roomIndex++));
                items.add(item);
            }

            if (items.size() >= MAX_GENERATED_ITEMS) break;
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
            defaults.add(defaultWindow(day, LocalTime.of(8, 0),
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
            List<String> warnings) {
        try {
            Result<List<DoctorSchedule>> result =
                    adminFeignClient.getDoctorSchedulesForAI(
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

    private List<DoctorSchedule> fetchRoomUsages(
            AiScheduleGenerateRequest request,
            List<String> warnings) {
        List<String> rooms = nullToEmpty(request.getRooms()).stream()
                .filter(StringUtils::hasText)
                .toList();
        if (rooms.isEmpty()) {
            return List.of();
        }
        try {
            Result<List<DoctorSchedule>> result =
                    adminFeignClient.getRoomUsageForAI(
                            rooms,
                            request.getPeriodStart().toString(),
                            request.getPeriodEnd().toString());
            if (ResultCode.SUCCESS.equals(result.getCode())) {
                return result.getData() == null ? List.of() : result.getData();
            }
            warnings.add("Failed to read room usages: "
                    + valueOrDefault(result.getMsg(), "admin-service error"));
        } catch (Exception exception) {
            warnings.add("Failed to read room usages: "
                    + exception.getClass().getSimpleName());
        }
        return List.of();
    }

    private void markConflicts(
            List<AiScheduleItem> items,
            List<String> warnings) {
        for (AiScheduleItem item : nullToEmpty(items)) {
            try {
                Result<ScheduleConflictResult> result =
                        adminFeignClient.checkConflict(toDoctorSchedule(item));
                if (ResultCode.SUCCESS.equals(result.getCode())) {
                    ScheduleConflictResult conflictResult = result.getData();
                    boolean conflict = conflictResult != null
                            && conflictResult.isConflict();
                    item.setConflict(conflict);
                    item.setConflictType(conflict && conflictResult != null
                            ? conflictResult.getConflictType() : null);
                    item.setConflictReason(conflict && conflictResult != null
                            ? conflictResult.getConflictReason() : null);
                } else {
                    item.setConflict(true);
                    item.setConflictType("CHECK_FAILED");
                    item.setConflictReason("Conflict check failed.");
                    warnings.add("Conflict check failed: " + valueOrDefault(
                            result.getMsg(), "admin-service error"));
                }
            } catch (Exception exception) {
                item.setConflict(true);
                item.setConflictType("CHECK_EXCEPTION");
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
package com.cloudbrainmed.ai.service.impl;

import com.cloudbrainmed.ai.dto.AiDepartmentRecommendationDto;
import com.cloudbrainmed.ai.dto.ConsultRecommendDto;
import com.cloudbrainmed.ai.entity.Doctor;
import com.cloudbrainmed.ai.entity.Department;
import com.cloudbrainmed.ai.mapper.DoctorMapper;
import com.cloudbrainmed.ai.mapper.DepartmentMapper;
import com.cloudbrainmed.ai.service.AiConsultService;
import com.cloudbrainmed.ai.tool.RagKnowledgeTool;
import com.cloudbrainmed.ai.vo.AiRecommendResponseVo;
import com.cloudbrainmed.ai.vo.RecommendDoctorVo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AI智能问诊服务实现
 * 新增内存临时会话记忆，支持多轮连贯问诊
 */
@Service
@Slf4j
public class AiConsultServiceImpl implements AiConsultService {

    // ===================== 新增：内存临时会话缓存 =====================
    // key: sessionId 会话标识  value: 当前会话完整对话消息列表
    private final ConcurrentHashMap<String, List<Message>> sessionMemory = new ConcurrentHashMap<>();

    private final ChatClient chatClient;
    private final DoctorMapper doctorMapper;
    private final DepartmentMapper departmentMapper;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final RagKnowledgeTool ragKnowledgeTool;

    // ==================== 提示词缓存 ====================
    private String globalSystemPrompt;
    private String consultationSystemPrompt;

    // ==================== 配置参数 ====================
    @Value("${spring.ai.openai.chat.options.model:deepseek-v4-flash}")
    private String modelName;

    @Value("${spring.ai.openai.chat.options.temperature:0.3}")
    private Double temperature;

    @Value("${spring.ai.openai.chat.options.max-tokens:800}")
    private Integer maxTokens;

    // ==================== 构造器注入 ====================
    public AiConsultServiceImpl(
            ChatClient chatClient,
            DoctorMapper doctorMapper,
            DepartmentMapper departmentMapper,
            ResourceLoader resourceLoader,
            RagKnowledgeTool ragKnowledgeTool,
            ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.doctorMapper = doctorMapper;
        this.departmentMapper = departmentMapper;
        this.resourceLoader = resourceLoader;
        this.ragKnowledgeTool = ragKnowledgeTool;
        this.objectMapper = objectMapper;
    }

    // ==================== 职称权重配置 ====================
    private static final Map<String, Integer> POSITION_WEIGHT = new HashMap<>();

    static {
        POSITION_WEIGHT.put("主任医师", 100);
        POSITION_WEIGHT.put("副主任医师", 90);
        POSITION_WEIGHT.put("主治医师", 80);
        POSITION_WEIGHT.put("住院医师", 70);
        POSITION_WEIGHT.put("医师", 60);
        POSITION_WEIGHT.put("主任", 95);
        POSITION_WEIGHT.put("副主任", 85);
        POSITION_WEIGHT.put("科室主任", 95);
        POSITION_WEIGHT.put("教授", 95);
        POSITION_WEIGHT.put("副教授", 85);
    }

    // ==================== 缓存变量 ====================
    private Map<String, List<String>> departmentRelationMap;
    private List<String> allDepartmentNames;

    // ==================== 初始化 ====================
    @PostConstruct
    public void init() {
        loadPromptFiles();
        log.info("✅ AiConsultService 初始化完成（Tool模式+内存会话记忆），全局人设长度: {} 字符，科室推荐提示词长度: {} 字符",
                globalSystemPrompt != null ? globalSystemPrompt.length() : 0,
                consultationSystemPrompt != null ? consultationSystemPrompt.length() : 0);
    }

    private void loadPromptFiles() {
        this.globalSystemPrompt = loadPrompt("classpath:prompt/defaultsystem.st");
        if (!StringUtils.hasText(globalSystemPrompt)) {
            log.warn("全局人设加载失败，使用默认值");
            this.globalSystemPrompt = getDefaultGlobalPrompt();
        }
        this.consultationSystemPrompt = loadPrompt("classpath:prompt/consultation_system.st");
        if (!StringUtils.hasText(consultationSystemPrompt)) {
            log.warn("科室推荐提示词加载失败，使用默认值");
            this.consultationSystemPrompt = getDefaultConsultationPrompt();
        }
    }

    private String loadPrompt(String path) {
        try {
            Resource resource = resourceLoader.getResource(path);
            if (resource.exists()) {
                String content = resource.getContentAsString(StandardCharsets.UTF_8);
                if (StringUtils.hasText(content)) {
                    log.info("✅ 成功加载提示词文件: {}", path);
                    return content;
                }
            }
            log.warn("⚠️ 提示词文件不存在或为空: {}", path);
            return "";
        } catch (IOException e) {
            log.error("❌ 读取提示词文件失败: {}", path, e);
            return "";
        }
    }

    private String getDefaultGlobalPrompt() {
        return """
                你是一位拥有15年临床经验的智慧医疗AI助手。
                你的核心使命是辅助医护人员提升诊疗效率，为患者提供专业、安全、有温度的智能医疗服务。
                你具备三甲医院主治医师级别的医学知识储备。
                绝不替代执业医师进行最终诊断，绝不提供具体药品处方。
                所有建议必须标注"建议由执业医师确认"。
                危重症状必须明确提示"请立即就医"。
                """;
    }

    private String getDefaultConsultationPrompt() {
        return """
                你是一位三甲医院全科主治医师，拥有10年临床经验。
                你的任务是根据患者主诉，从科室列表中选择最匹配的科室。

                推荐逻辑：
                1. 一级推荐（精准匹配）：症状典型，直接对应科室
                2. 二级推荐（相关科室）：症状不典型，推荐相关科室

                常见症状映射：
                - 发热+咳嗽+咽痛 → 呼吸内科
                - 胸痛+心悸+气短 → 心血管内科
                - 腹痛+腹泻+恶心 → 消化内科
                - 头痛+头晕+恶心 → 神经内科
                - 关节痛+肿胀 → 骨科
                - 腰痛+腿麻 → 骨科
                - 尿频+尿急+尿痛 → 泌尿外科
                - 皮疹+瘙痒 → 皮肤科
                - 失眠+焦虑+抑郁 → 心理科
                - 儿童症状 → 儿科
                - 孕妇不适 → 妇产科
                - 胸痛+大汗+濒死感 → 急诊科
                """;
    }

    // ==================== 会话记忆工具方法 ====================
    /**
     * 获取当前会话历史消息，不存在则新建空列表
     */
    private List<Message> getSessionHistory(String sessionId) {
        return sessionMemory.computeIfAbsent(sessionId, k -> new ArrayList<>());
    }

    /**
     * 保存一轮用户+AI对话到内存记忆
     */
    private void saveSessionMessage(String sessionId, UserMessage userMsg, AssistantMessage assistantMsg) {
        List<Message> history = getSessionHistory(sessionId);
        history.add(userMsg);
        history.add(assistantMsg);
        log.info("会话{}，已保存多轮对话，累计消息数：{}", sessionId, history.size());
    }

    /**
     * 清空指定会话记忆
     */
    @Override
    public void clearSessionMemory(String sessionId) {
        sessionMemory.remove(sessionId);
        log.info("已清空会话{}临时记忆", sessionId);
    }

    // ==================== 核心业务方法（新增sessionId多轮问诊） ====================
    @Override
    public AiRecommendResponseVo recommendDoctor(String sessionId, ConsultRecommendDto consultRecommendDto) {
        String chiefComplaint = consultRecommendDto.getChiefComplaint();
        List<String> deptNames = getAllDepartmentNamesFromDB();
        String deptList = String.join("、", deptNames);
        String combinedSystemPrompt = buildToolSystemPrompt();

        // 传入会话ID，携带历史多轮上下文分析
        AiDepartmentRecommendationDto aiResult = analyzeWithTools(sessionId, chiefComplaint, deptList, combinedSystemPrompt);
        String recommendedDept = aiResult.getRecommendedDepartment();

        Department department = departmentMapper.selectByDeptName(recommendedDept);
        // 匹配不到科室，自动切换有医生的兜底科室
        if (department == null) {
            String defaultDept = getDefaultDepartmentWithDoctor();
            department = departmentMapper.selectByDeptName(defaultDept);
            if (department == null) {
                return AiRecommendResponseVo.builder()
                        .parsedDiagnosis(aiResult.getParsedDiagnosis())
                        .recommendedDepartment(recommendedDept)
                        .departmentReason("系统暂无对应科室，请联系管理员")
                        .doctorRanking(new ArrayList<>())
                        .aiAnalysisTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        .emergency(aiResult.getEmergency())
                        .build();
            }
        }

        List<Doctor> doctors = doctorMapper.selectByDepartmentId(department.getDeptId());
        log.info("科室【{}】下共有 {} 位医生", department.getDeptName(), doctors.size());
        List<RecommendDoctorVo> rankedDoctors = calculateMatchScores(doctors, chiefComplaint);

        return AiRecommendResponseVo.builder()
                .parsedDiagnosis(aiResult.getParsedDiagnosis())
                .recommendedDepartment(department.getDeptName())
                .departmentReason(aiResult.getDepartmentReason())
                .doctorRanking(rankedDoctors)
                .aiAnalysisTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .emergency(aiResult.getEmergency())
                .build();
    }

    // 兼容旧接口，废弃单轮无记忆调用
    @Override
    public AiRecommendResponseVo recommendDoctor(ConsultRecommendDto consultRecommendDto) {
        // 生成临时随机sessionId，单轮独立会话
        String tempSession = UUID.randomUUID().toString().substring(0, 16);
        return recommendDoctor(tempSession, consultRecommendDto);
    }

    @Override
    public List<String> getAllDepartments() {
        return getAllDepartmentNamesFromDB();
    }

    // ==================== Tool分析核心（新增sessionId，携带历史消息） ====================
    private AiDepartmentRecommendationDto analyzeWithTools(String sessionId, String chiefComplaint, String deptList, String systemPrompt) {
        try {
            String userPrompt = buildToolUserPrompt(chiefComplaint, deptList);
            List<Message> historyMsgList = getSessionHistory(sessionId);
            log.info("🤖 会话{}启动Tool模式AI多轮分析，当前主诉: {}，历史对话数量: {}", sessionId, chiefComplaint, historyMsgList.size());

            // 修改：直接传入字符串内容，而不是 UserMessage 对象
            String responseStr = chatClient.prompt()
                    .system(systemPrompt)
                    .messages(historyMsgList) // 注入历史会话记忆
                    .user(userPrompt)  // 直接传入字符串，而不是 (Resource) currentUserMsg
                    .options(OpenAiChatOptions.builder()
                            .model(modelName)
                            .temperature(temperature)
                            .maxTokens(maxTokens)
                            .build())
                    .tools(ragKnowledgeTool)
                    .call()
                    .content();

            log.info("🤖 Tool模式AI原始响应: {}", responseStr);
            AiDepartmentRecommendationDto dto = parseResponse(responseStr);

            // 保存本轮对话到内存
            UserMessage currentUserMsg = new UserMessage(userPrompt);
            AssistantMessage assistantMsg = new AssistantMessage(responseStr);
            saveSessionMessage(sessionId, currentUserMsg, assistantMsg);

            return dto;
        } catch (Exception e) {
            log.error("Tool模式AI分析失败", e);
            return getFallbackResult();
        }
    }

    private String buildToolSystemPrompt() {
        StringBuilder combined = new StringBuilder();
        if (StringUtils.hasText(globalSystemPrompt)) {
            combined.append(globalSystemPrompt);
        }
        combined.append("\n\n---\n\n");
        if (StringUtils.hasText(consultationSystemPrompt)) {
            combined.append(consultationSystemPrompt);
        }

        combined.append("\n\n【工具使用说明】\n");
        combined.append("你可以使用 retrieveMedicalKnowledge 检索医学知识库辅助判断。\n");
        combined.append("当前为多轮问诊，必须结合用户全部历史症状综合判断，不要忽略之前描述的不适。\n");

        // 强制约束，杜绝多余文字、外层包装
        combined.append("\n\n【强制输出规则，违反则结果无效】\n");
        combined.append("1. 禁止输出任何解释、分析、前言、总结、markdown、```标记；\n");
        combined.append("2. 禁止包装 code、message、disclaimer、data 等外层字段；\n");
        combined.append("3. 仅输出纯JSON，字段严格如下，不能增减：\n");
        combined.append("{\"parsed_diagnosis\":\"\",\"recommended_department\":\"\",\"department_reason\":\"\",\"emergency\":false}\n");
        combined.append("4. recommended_department 必须从提供的科室列表中选取；\n");
        combined.append("5. parsed_diagnosis 整合全部历史症状，20字以内，department_reason 30字以内。");
        return combined.toString();
    }

    private String buildToolUserPrompt(String chiefComplaint, String deptList) {
        return String.format("""
                【患者本次新增主诉】
                %s

                【系统可用科室列表】
                %s

                任务：结合用户全部历史症状综合提取病情，匹配科室，必要时调用知识库工具。
                严格遵守上方输出规则，只返回标准JSON，不要任何附加文字。
                """, chiefComplaint, deptList);
    }

    // 增强解析：兼容多余文字、markdown、外层data包装
    private AiDepartmentRecommendationDto parseResponse(String response) {
        try {
            String cleanJson = extractPureJson(response);
            return objectMapper.readValue(cleanJson, AiDepartmentRecommendationDto.class);
        } catch (Exception e) {
            log.warn("直接解析JSON失败，尝试兜底", e);
            return getFallbackResult();
        }
    }

    private String extractPureJson(String response) throws Exception {
        if (!StringUtils.hasText(response)) {
            throw new RuntimeException("空响应");
        }
        // 清除代码块标记
        String raw = response.replaceAll("```json", "").replaceAll("```", "").trim();
        int jsonStart = raw.indexOf('{');
        int jsonEnd = raw.lastIndexOf('}');
        if (jsonStart < 0 || jsonEnd <= jsonStart) {
            throw new RuntimeException("无有效JSON");
        }
        String jsonStr = raw.substring(jsonStart, jsonEnd + 1);

        // 判断是否外层套了data（如{"code":200,"data":{...}}）
        Map<String, Object> rootMap = objectMapper.readValue(jsonStr, new TypeReference<>() {});
        if (rootMap.containsKey("data")) {
            return objectMapper.writeValueAsString(rootMap.get("data"));
        }
        return jsonStr;
    }

    // ==================== 科室缓存 & 优化兜底逻辑 ====================
    private List<String> getAllDepartmentNamesFromDB() {
        if (allDepartmentNames == null || allDepartmentNames.isEmpty()) {
            allDepartmentNames = departmentMapper.selectAllDeptNames();
            log.info("从数据库加载科室列表，共 {} 个科室", allDepartmentNames.size());
        }
        return allDepartmentNames;
    }

    /** 优化兜底：自动找存在医生的科室，避免返回空医生列表 */
    private String getDefaultDepartmentWithDoctor() {
        List<String> deptNames = getAllDepartmentNamesFromDB();
        if (deptNames.isEmpty()) {
            return "全科";
        }
        for (String deptName : deptNames) {
            // 参数用循环变量 deptName，接收对象改名 deptObj 避免重名
            Department deptObj = departmentMapper.selectByDeptName(deptName);
            if (deptObj != null) {
                List<Doctor> docList = doctorMapper.selectByDepartmentId(deptObj.getDeptId());
                if (!docList.isEmpty()) {
                    return deptName;
                }
            }
        }
        // 所有科室都无医生，返回第一个
        return deptNames.get(0);
    }

    // 旧方法保留兼容
    private String getDefaultDepartment() {
        return getDefaultDepartmentWithDoctor();
    }

    private Map<String, List<String>> getDepartmentRelationMap() {
        if (departmentRelationMap == null) {
            departmentRelationMap = buildDepartmentRelationMap();
            log.info("从数据库构建科室关联关系，共 {} 个科室有关联", departmentRelationMap.size());
        }
        return departmentRelationMap;
    }

    private Map<String, List<String>> buildDepartmentRelationMap() {
        Map<String, List<String>> relationMap = new HashMap<>();
        List<Department> allDepts = departmentMapper.selectAllDepartments();
        for (Department dept : allDepts) {
            String deptName = dept.getDeptName();
            List<String> related = new ArrayList<>();
            if (deptName.contains("内科")) {
                for (String name : allDepartmentNames) {
                    if (name.contains("内科") && !name.equals(deptName)) related.add(name);
                }
            }
            if (deptName.contains("外科")) {
                for (String name : allDepartmentNames) {
                    if (name.contains("外科") && !name.equals(deptName)) related.add(name);
                }
            }
            String keyword = extractKeyword(deptName);
            if (StringUtils.hasText(keyword) && !keyword.equals(deptName)) {
                for (String name : allDepartmentNames) {
                    if (name.contains(keyword) && !name.equals(deptName)) related.add(name);
                }
            }
            if (!related.isEmpty()) relationMap.put(deptName, related);
        }
        return relationMap;
    }

    private String extractKeyword(String deptName) {
        if (deptName.endsWith("科") && deptName.length() > 2) {
            return deptName.substring(0, deptName.length() - 1);
        }
        if (deptName.contains("科")) {
            return deptName.substring(0, deptName.indexOf("科"));
        }
        return deptName;
    }

    private AiDepartmentRecommendationDto getFallbackResult() {
        String safeDept = getDefaultDepartmentWithDoctor();
        return AiDepartmentRecommendationDto.builder()
                .parsedDiagnosis("根据您的描述，建议进一步就医检查")
                .recommendedDepartment(safeDept)
                .departmentReason("AI分析异常，为您匹配现有可就诊科室")
                .emergency(false)
                .build();
    }

    // ==================== 医生匹配评分 ====================
    private List<RecommendDoctorVo> calculateMatchScores(List<Doctor> doctors, String chiefComplaint) {
        if (doctors == null || doctors.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, String> deptIdToName = departmentMapper.getDeptIdToNameMap();
        List<RecommendDoctorVo> result = new ArrayList<>();
        for (Doctor doctor : doctors) {
            if (doctor.getStatus() == null || doctor.getStatus() != 1) continue;
            if (doctor.getIsDeleted() != null && doctor.getIsDeleted() == 1) continue;
            BigDecimal matchScore = calculateDoctorMatchScore(doctor, chiefComplaint);
            String departmentName = deptIdToName.getOrDefault(doctor.getDepartmentId(), "未知科室");
            RecommendDoctorVo vo = RecommendDoctorVo.builder()
                    .doctorId(doctor.getDoctorId())
                    .name(doctor.getName())
                    .position(doctor.getPosition())
                    .goodAt(doctor.getGoodAt())
                    .introduction(doctor.getIntroduction())
                    .avatar(doctor.getAvatar())
                    .departmentId(doctor.getDepartmentId())
                    .departmentName(departmentName)
                    .matchScore(matchScore)
                    .matchReason(buildMatchReason(doctor, matchScore))
                    .build();
            result.add(vo);
        }
        return result.stream()
                .sorted((a, b) -> b.getMatchScore().compareTo(a.getMatchScore()))
                .collect(Collectors.toList());
    }

    private BigDecimal calculateDoctorMatchScore(Doctor doctor, String chiefComplaint) {
        BigDecimal positionScore = calculatePositionMatch(doctor);
        BigDecimal goodAtScore = calculateGoodAtMatch(doctor, chiefComplaint);
        BigDecimal introScore = calculateIntroductionMatch(doctor, chiefComplaint);
        return positionScore.add(goodAtScore).add(introScore)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePositionMatch(Doctor doctor) {
        String position = doctor.getPosition();
        if (!StringUtils.hasText(position)) return new BigDecimal("15");
        for (Map.Entry<String, Integer> entry : POSITION_WEIGHT.entrySet()) {
            if (position.contains(entry.getKey())) {
                return new BigDecimal(entry.getValue() * 0.3);
            }
        }
        return new BigDecimal("15");
    }

    private BigDecimal calculateGoodAtMatch(Doctor doctor, String chiefComplaint) {
        String goodAt = doctor.getGoodAt();
        if (!StringUtils.hasText(goodAt) || !StringUtils.hasText(chiefComplaint)) return BigDecimal.ZERO;
        List<String> keywords = extractKeywords(chiefComplaint);
        int matchCount = 0;
        for (String keyword : keywords) {
            if (goodAt.contains(keyword)) matchCount++;
        }
        return new BigDecimal(Math.min(matchCount * 5, 20));
    }

    private BigDecimal calculateIntroductionMatch(Doctor doctor, String chiefComplaint) {
        String introduction = doctor.getIntroduction();
        if (!StringUtils.hasText(introduction) || !StringUtils.hasText(chiefComplaint)) return BigDecimal.ZERO;
        List<String> keywords = extractKeywords(chiefComplaint);
        int matchCount = 0;
        for (String keyword : keywords) {
            if (introduction.contains(keyword)) matchCount++;
        }
        return new BigDecimal(Math.min(matchCount * 2, 10));
    }

    private List<String> extractKeywords(String text) {
        List<String> keywords = new ArrayList<>();
        String[] symptomKeywords = {"发热", "咳嗽", "头痛", "腹痛", "恶心", "呕吐", "腹泻", "便秘",
                "胸痛", "心悸", "气短", "头晕", "乏力", "皮疹", "瘙痒", "关节痛",
                "腰痛", "背痛", "失眠", "焦虑", "抑郁", "视力模糊", "耳鸣"};
        for (String keyword : symptomKeywords) {
            if (text.contains(keyword)) keywords.add(keyword);
        }
        return keywords;
    }

    private String buildMatchReason(Doctor doctor, BigDecimal matchScore) {
        List<String> reasons = new ArrayList<>();
        if (StringUtils.hasText(doctor.getPosition())) reasons.add("职称：" + doctor.getPosition());
        if (StringUtils.hasText(doctor.getGoodAt())) {
            String shortGoodAt = doctor.getGoodAt().length() > 30 ?
                    doctor.getGoodAt().substring(0, 30) + "..." : doctor.getGoodAt();
            reasons.add("擅长：" + shortGoodAt);
        }
        if (matchScore.compareTo(new BigDecimal("80")) >= 0) {
            reasons.add("高度匹配");
        } else if (matchScore.compareTo(new BigDecimal("60")) >= 0) {
            reasons.add("较好匹配");
        }
        return String.join("；", reasons);
    }
}
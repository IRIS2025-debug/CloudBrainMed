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
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
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
import java.util.stream.Collectors;

/**
 * AI智能问诊服务
 * 支持多症状返回多个细分科室，自动匹配院内大类科室
 */
@Service
@Slf4j
public class AiConsultServiceImpl implements AiConsultService {

    private final ChatClient chatClient;
    private final DoctorMapper doctorMapper;
    private final DepartmentMapper departmentMapper;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final RagKnowledgeTool ragKnowledgeTool;

    // ==================== 提示词缓存 ====================
    private String globalSystemPrompt;
    private String consultationSystemPrompt;

    // ==================== 配置参数（增大超时相关配置） ====================
    @Value("${spring.ai.openai.chat.options.model:deepseek-v4-flash}")
    private String modelName;

    @Value("${spring.ai.openai.chat.options.temperature:0.3}")
    private Double temperature;

    @Value("${spring.ai.openai.chat.options.max-tokens:800}")
    private Integer maxTokens;

    // 新增：超时时间配置
    @Value("${spring.ai.openai.timeout:300000}")
    private long timeoutMillis;

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
        log.info("✅ AiConsultService 初始化完成（无会话记忆，每轮独立问诊）");
        log.info("✅ AI问诊超时配置: {}ms, 模型: {}, maxTokens: {}", timeoutMillis, modelName, maxTokens);
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
                你是一位拥有15年临床经验的三甲医院AI问诊助手。
                仅根据用户当前单次输入的主诉分析，无任何历史对话参考。
                不能替代执业医师诊断，所有就诊建议标注需医师确认；出现剧烈胸痛、大出血、意识丧失等急症提示立刻前往急诊。
                """;
    }

    private String getDefaultConsultationPrompt() {
        return """
                按用户本次主诉匹配标准医学细分科室；多处不同部位症状必须输出多个科室放入数组。
                固定映射规则：
                腹痛/肚子疼 → 消化内科；皮肤红痒皮疹 → 皮肤科；脖子/关节痛 → 骨科；眼痛眼干 → 眼科；头痛头晕 → 神经内科
                细分内科专科统一归属内科大类；骨科、普外科等外科细分归属外科大类。
                """;
    }

    // ==================== 清空会话接口（空实现，无缓存） ====================
    @Override
    public void clearSessionMemory(String sessionId) {
        log.info("当前服务无会话记忆缓存，无需清空操作");
    }

    // ==================== 细分科室匹配院内大类工具 ====================
    private String matchHospitalRealDept(String aiStandardDept, List<String> hospitalDeptList) {
        // 1. 精确匹配优先
        if (hospitalDeptList.contains(aiStandardDept)) {
            return aiStandardDept;
        }
        // 2. 细分科室映射大类
        Map<String, String> subToBigMap = new HashMap<>();
        // 内科细分映射内科大类
        subToBigMap.put("消化内科", "内科");
        subToBigMap.put("呼吸内科", "内科");
        subToBigMap.put("心血管内科", "内科");
        subToBigMap.put("神经内科", "内科");
        subToBigMap.put("肾内科", "内科");
        subToBigMap.put("内分泌科", "内科");
        // 外科细分映射外科大类
        subToBigMap.put("骨科", "外科");
        subToBigMap.put("普外科", "外科");
        subToBigMap.put("泌尿外科", "外科");
        subToBigMap.put("心胸外科", "外科");

        if (subToBigMap.containsKey(aiStandardDept)) {
            String bigDept = subToBigMap.get(aiStandardDept);
            if (hospitalDeptList.contains(bigDept)) {
                return bigDept;
            }
        }
        // 3. 无匹配
        return null;
    }

    // ==================== 核心业务方法 ====================
    @Override
    public AiRecommendResponseVo recommendDoctor(String sessionId, ConsultRecommendDto consultRecommendDto) {
        String chiefComplaint = consultRecommendDto.getChiefComplaint();
        List<String> deptNames = getAllDepartmentNamesFromDB();
        String deptList = String.join("、", deptNames);
        String combinedSystemPrompt = buildToolSystemPrompt();

        // AI仅分析本次输入文本，空上下文，无历史
        AiDepartmentRecommendationDto aiResult = analyzeSingleDept(chiefComplaint, deptList, combinedSystemPrompt);
        List<String> aiStandardDeptList = aiResult.getRecommendedDepartments();
        if (aiStandardDeptList == null || aiStandardDeptList.isEmpty()) {
            aiStandardDeptList = Collections.singletonList("全科");
        }
        log.info("【单次独立问诊】AI输出细分科室数组:{}，院内科室列表:{}", aiStandardDeptList, deptNames);

        List<String> existHospitalDept = new ArrayList<>();
        List<String> noExistStandardDept = new ArrayList<>();
        Map<String, String> subToRealMap = new HashMap<>();

        // 循环匹配每个AI推荐细分科室
        for (String subDept : aiStandardDeptList) {
            String realDept = matchHospitalRealDept(subDept, deptNames);
            if (StringUtils.hasText(realDept)) {
                existHospitalDept.add(realDept);
                subToRealMap.put(subDept, realDept);
            } else {
                noExistStandardDept.add(subDept);
            }
        }
        log.info("本院可就诊科室:{}，无对应科室:{}", existHospitalDept, noExistStandardDept);

        // 拼接前端展示说明文案
        StringBuilder reasonSb = new StringBuilder();
        reasonSb.append(aiResult.getDepartmentReason()).append("\n");

        if (!existHospitalDept.isEmpty()) {
            reasonSb.append("✅ 本院可就诊科室：");
            List<String> showTips = new ArrayList<>();
            for (Map.Entry<String, String> entry : subToRealMap.entrySet()) {
                String sub = entry.getKey();
                String real = entry.getValue();
                if (sub.equals(real)) {
                    showTips.add(sub);
                } else {
                    showTips.add(sub + "（本院无细分专科，可就诊" + real + "）");
                }
            }
            reasonSb.append(String.join("、", showTips)).append("\n");
        }
        if (!noExistStandardDept.isEmpty()) {
            reasonSb.append("❌ 本院暂无对应科室：");
            reasonSb.append(String.join("、", noExistStandardDept));
            reasonSb.append("，建议更换其他医院就诊");
        }

        // 合并所有匹配科室医生，全局匹配度排序取前5
        List<String> distinctRealDept = existHospitalDept.stream().distinct().collect(Collectors.toList());
        List<RecommendDoctorVo> allDoctorList = new ArrayList<>();
        for (String realDept : distinctRealDept) {
            Department department = departmentMapper.selectByDeptName(realDept);
            List<Doctor> doctors = doctorMapper.selectByDepartmentId(department.getDeptId());
            List<RecommendDoctorVo> singleDeptDoctor = calculateMatchScores(doctors, chiefComplaint);
            allDoctorList.addAll(singleDeptDoctor);
        }
        List<RecommendDoctorVo> top5Doctors = allDoctorList.stream()
                .sorted((a, b) -> b.getMatchScore().compareTo(a.getMatchScore()))
                .limit(5)
                .collect(Collectors.toList());

        String showMainDept = String.join("、", distinctRealDept);
        if (StringUtils.isEmpty(showMainDept)) {
            showMainDept = String.join("、", noExistStandardDept);
        }

        return AiRecommendResponseVo.builder()
                .parsedDiagnosis(aiResult.getParsedDiagnosis())
                .recommendedDepartment(showMainDept)
                .departmentReason(reasonSb.toString())
                .doctorRanking(top5Doctors)
                .aiAnalysisTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .emergency(aiResult.getEmergency())
                .build();
    }

    // ==================== AI单次问诊调用（增加超时处理） ====================
    private AiDepartmentRecommendationDto analyzeSingleDept(String chiefComplaint, String deptList, String systemPrompt) {
        long startTime = System.currentTimeMillis();
        try {
            String userPrompt = buildSingleUserPrompt(chiefComplaint, deptList);
            List<Message> emptyHistory = new ArrayList<>();
            log.info("🤖 单次独立问诊启动AI分析，当前主诉: {}，无任何历史对话", chiefComplaint);

            String responseStr = chatClient.prompt()
                    .system(systemPrompt)
                    .messages(emptyHistory)
                    .user(userPrompt)
                    .options(OpenAiChatOptions.builder()
                            .model(modelName)
                            .temperature(temperature)
                            .maxTokens(maxTokens)
                            .build())
                    .tools(ragKnowledgeTool)
                    .call()
                    .content();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("🤖 Tool模式AI响应完成，耗时: {}ms, 响应长度: {}", elapsed, responseStr.length());
            log.info("🤖 Tool模式AI原始响应: {}", responseStr);

            AiDepartmentRecommendationDto dto = parseResponse(responseStr);
            return dto;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("单次独立问诊AI分析失败，耗时: {}ms, 错误类型: {}", elapsed, e.getClass().getSimpleName(), e);

            // 判断是否是超时异常
            if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                log.warn("AI问诊超时，使用降级方案。超时配置: {}ms", timeoutMillis);
            }

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

        combined.append("\n【关键规则】仅分析用户本次输入文字，不存在任何历史对话记录；多处症状必须输出多个细分科室数组。\n");
        combined.append("输出固定JSON格式，禁止任何markdown、注释、多余文字：\n");
        combined.append("{\"parsed_diagnosis\":\"\",\"recommended_departments\":[\"科室1\",\"科室2\"],\"department_reason\":\"\",\"emergency\":false}");
        return combined.toString();
    }

    private String buildSingleUserPrompt(String chiefComplaint, String deptList) {
        return String.format("""
                患者单次主诉：%s
                本院科室列表（仅作参考，不限制科室输出）：%s
                要求：多处不同部位症状输出多个细分科室到recommended_departments数组，单一症状数组仅一个值；只返回纯净JSON文本。
                """, chiefComplaint, deptList);
    }

    // JSON清洗解析工具
    private AiDepartmentRecommendationDto parseResponse(String response) {
        try {
            String cleanJson = extractPureJson(response);
            log.info("清洗后JSON字符串:{}", cleanJson);
            AiDepartmentRecommendationDto dto = objectMapper.readValue(cleanJson, AiDepartmentRecommendationDto.class);
            log.info("JSON解析完成，AI推荐科室数组:{}", dto.getRecommendedDepartments());
            return dto;
        } catch (Exception e) {
            log.warn("JSON解析失败，使用全科兜底返回", e);
            return getFallbackResult();
        }
    }

    private String extractPureJson(String response) {
        if (!StringUtils.hasText(response)) return "";
        String raw = response.replaceAll("```json", "").replaceAll("```", "").trim();
        int jsonStart = raw.indexOf('{');
        int jsonEnd = raw.lastIndexOf('}');
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return raw.substring(jsonStart, jsonEnd + 1);
        }
        return raw;
    }

    // ==================== 院内科室缓存加载 ====================
    private List<String> getAllDepartmentNamesFromDB() {
        if (allDepartmentNames == null || allDepartmentNames.isEmpty()) {
            allDepartmentNames = departmentMapper.selectAllDeptNames();
            log.info("加载院内科室列表，共{}个科室：{}", allDepartmentNames.size(), allDepartmentNames);
        }
        return allDepartmentNames;
    }

    private Map<String, List<String>> getDepartmentRelationMap() {
        if (departmentRelationMap == null) {
            departmentRelationMap = buildDepartmentRelationMap();
            log.info("构建科室关联映射，存在关联科室共{}组", departmentRelationMap.size());
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
                allDepartmentNames.stream()
                        .filter(name -> name.contains("内科") && !name.equals(deptName))
                        .forEach(related::add);
            }
            if (deptName.contains("外科")) {
                allDepartmentNames.stream()
                        .filter(name -> name.contains("外科") && !name.equals(deptName))
                        .forEach(related::add);
            }
            String keyword = extractKeyword(deptName);
            if (StringUtils.hasText(keyword)) {
                allDepartmentNames.stream()
                        .filter(name -> name.contains(keyword) && !name.equals(deptName))
                        .forEach(related::add);
            }
            if (!related.isEmpty()) {
                relationMap.put(deptName, related);
            }
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

    // AI解析异常兜底返回
    private AiDepartmentRecommendationDto getFallbackResult() {
        return AiDepartmentRecommendationDto.builder()
                .parsedDiagnosis("症状解析异常")
                .recommendedDepartments(Collections.singletonList("全科"))
                .departmentReason("AI解析失败，建议前往全科初步就诊")
                .emergency(false)
                .build();
    }

    // ==================== 医生匹配评分逻辑 ====================
    private List<RecommendDoctorVo> calculateMatchScores(List<Doctor> doctors, String chiefComplaint) {
        if (doctors == null || doctors.isEmpty()) return new ArrayList<>();
        Map<String, String> deptIdNameMap = departmentMapper.getDeptIdToNameMap();
        List<RecommendDoctorVo> result = new ArrayList<>();
        for (Doctor doc : doctors) {
            if (doc.getStatus() == null || doc.getStatus() != 1
                    || (doc.getIsDeleted() != null && doc.getIsDeleted() == 1)) {
                continue;
            }
            BigDecimal matchScore = calculateDoctorMatchScore(doc, chiefComplaint);
            String departmentName = deptIdNameMap.getOrDefault(doc.getDepartmentId(), "未知科室");
            RecommendDoctorVo vo = RecommendDoctorVo.builder()
                    .doctorId(doc.getDoctorId())
                    .name(doc.getName())
                    .position(doc.getPosition())
                    .goodAt(doc.getGoodAt())
                    .introduction(doc.getIntroduction())
                    .avatar(doc.getAvatar())
                    .departmentId(doc.getDepartmentId())
                    .departmentName(departmentName)
                    .matchScore(BigDecimal.valueOf(matchScore.intValue()))
                    .matchReason(buildMatchReason(doc, matchScore))
                    .build();
            result.add(vo);
        }
        return result.stream()
                .sorted((a, b) -> b.getMatchScore().compareTo(a.getMatchScore()))
                .collect(Collectors.toList());
    }

    private BigDecimal calculateDoctorMatchScore(Doctor doctor, String chiefComplaint) {
        BigDecimal posScore = calculatePositionMatch(doctor);
        BigDecimal goodAtScore = calculateGoodAtMatch(doctor, chiefComplaint);
        BigDecimal introScore = calculateIntroductionMatch(doctor, chiefComplaint);
        return posScore.add(goodAtScore).add(introScore)
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
        List<String> keywords = extractSymptomKeywords(chiefComplaint);
        long hitCount = keywords.stream().filter(goodAt::contains).count();
        return new BigDecimal(Math.min(hitCount * 5, 20));
    }

    private BigDecimal calculateIntroductionMatch(Doctor doctor, String chiefComplaint) {
        String intro = doctor.getIntroduction();
        if (!StringUtils.hasText(intro) || !StringUtils.hasText(chiefComplaint)) return BigDecimal.ZERO;
        List<String> keywords = extractSymptomKeywords(chiefComplaint);
        long hitCount = keywords.stream().filter(intro::contains).count();
        return new BigDecimal(Math.min(hitCount * 2, 10));
    }

    private List<String> extractSymptomKeywords(String text) {
        List<String> keywords = new ArrayList<>();
        String[] symptomArr = {
                "发热", "咳嗽", "头痛", "腹痛", "肚子疼", "恶心", "呕吐", "腹泻", "便秘",
                "胸痛", "心悸", "气短", "头晕", "乏力", "皮疹", "瘙痒", "发红", "皮肤红",
                "关节痛", "脖子疼", "眼痛", "眼睛疼", "腰痛", "背痛", "失眠", "焦虑", "视力模糊"
        };
        for (String word : symptomArr) {
            if (text.contains(word)) keywords.add(word);
        }
        return keywords;
    }

    private String buildMatchReason(Doctor doctor, BigDecimal matchScore) {
        List<String> tags = new ArrayList<>();
        if (StringUtils.hasText(doctor.getPosition())) tags.add("职称：" + doctor.getPosition());
        if (StringUtils.hasText(doctor.getGoodAt())) {
            String goodStr = doctor.getGoodAt().length() > 30
                    ? doctor.getGoodAt().substring(0, 30) + "..."
                    : doctor.getGoodAt();
            tags.add("擅长：" + goodStr);
        }
        if (matchScore.compareTo(new BigDecimal("80")) >= 0) {
            tags.add("高度匹配");
        } else if (matchScore.compareTo(new BigDecimal("60")) >= 0) {
            tags.add("较好匹配");
        }
        return String.join("；", tags);
    }
}
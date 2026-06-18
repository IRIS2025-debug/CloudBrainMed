package com.cloudbrainmed.ai.service.impl;

import com.cloudbrainmed.ai.dto.AiDepartmentRecommendationDto;  // 新导入
import com.cloudbrainmed.ai.dto.ConsultRecommendDto;
import com.cloudbrainmed.ai.entity.Doctor;
import com.cloudbrainmed.ai.entity.Department;
import com.cloudbrainmed.ai.mapper.DoctorMapper;
import com.cloudbrainmed.ai.mapper.DepartmentMapper;
import com.cloudbrainmed.ai.service.AiConsultService;
import com.cloudbrainmed.ai.vo.AiRecommendResponseVo;
import com.cloudbrainmed.ai.vo.RecommendDoctorVo;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
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

@Service
@Slf4j
public class AiConsultServiceImpl implements AiConsultService {

    private final ChatClient chatClient;
    private final DoctorMapper doctorMapper;
    private final DepartmentMapper departmentMapper;

    // 局部人设（问诊专用）
    @Value("classpath:prompt/consultation_system.st")
    private Resource consultationSystemResource;
    private String consultationSystemPrompt;  // 缓存局部人设

    // 构造器注入
    public AiConsultServiceImpl(ChatClient chatClient,
                                DoctorMapper doctorMapper,
                                DepartmentMapper departmentMapper) {
        this.chatClient = chatClient;
        this.doctorMapper = doctorMapper;
        this.departmentMapper = departmentMapper;
    }

    // 职称/职位权重配置
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

    // 科室关联关系缓存（从数据库加载）
    private Map<String, List<String>> departmentRelationMap;

    // 所有科室名称缓存
    private List<String> allDepartmentNames;

    @PostConstruct
    public void init() {
        try {
            // 加载局部人设
            this.consultationSystemPrompt = consultationSystemResource
                    .getContentAsString(StandardCharsets.UTF_8);
            log.info("✅ 加载问诊局部人设成功，长度: {} 字符", consultationSystemPrompt.length());
        } catch (IOException e) {
            log.error("❌ 加载问诊局部人设失败，使用默认人设", e);
            this.consultationSystemPrompt = """
                    你是一位三甲医院全科主治医师，拥有10年临床经验。
                    你的任务是根据患者主诉，从科室列表中选择最匹配的科室。
                    请严格按照JSON格式返回结果，不要包含任何其他文字。
                    """;
        }
    }

    @Override
    public AiRecommendResponseVo recommendDoctor(ConsultRecommendDto consultRecommendDto) {
        String chiefComplaint = consultRecommendDto.getChiefComplaint();

        // 1. 从数据库获取所有科室名称，构建提示词
        List<String> deptNames = getAllDepartmentNamesFromDB();

        // 2. AI解析主诉，获取推荐科室（结构化输出）
        AiDepartmentRecommendationDto aiResult = analyzeWithAI(chiefComplaint, deptNames);

        // 3. 获取所有未删除、启用的医生
        List<Doctor> allDoctors = doctorMapper.selectActiveDoctors();

        // 4. 计算每个医生的匹配分值
        List<RecommendDoctorVo> rankedDoctors = calculateMatchScores(allDoctors, aiResult.getRecommendedDepartment(), chiefComplaint);

        // 5. 构建返回结果
        return AiRecommendResponseVo.builder()
                .parsedDiagnosis(aiResult.getParsedDiagnosis())
                .recommendedDepartment(aiResult.getRecommendedDepartment())
                .departmentReason(aiResult.getDepartmentReason())
                .doctorRanking(rankedDoctors)
                .aiAnalysisTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .build();
    }

    @Override
    public List<String> getAllDepartments() {
        return getAllDepartmentNamesFromDB();
    }

    /**
     * 从数据库获取所有科室名称（带缓存）
     */
    private List<String> getAllDepartmentNamesFromDB() {
        if (allDepartmentNames == null || allDepartmentNames.isEmpty()) {
            allDepartmentNames = departmentMapper.selectAllDeptNames();
            log.info("从数据库加载科室列表，共 {} 个科室", allDepartmentNames.size());
        }
        return allDepartmentNames;
    }

    /**
     * 从数据库获取科室关联关系（带缓存）
     */
    private Map<String, List<String>> getDepartmentRelationMap() {
        if (departmentRelationMap == null) {
            departmentRelationMap = buildDepartmentRelationMap();
            log.info("从数据库构建科室关联关系，共 {} 个科室有关联", departmentRelationMap.size());
        }
        return departmentRelationMap;
    }

    /**
     * 从数据库构建科室关联关系
     * 规则：同一父科室下的子科室互相关联
     */
    private Map<String, List<String>> buildDepartmentRelationMap() {
        Map<String, List<String>> relationMap = new HashMap<>();

        // 获取所有科室
        List<Department> allDepts = departmentMapper.selectAllDepartments();

        // 按科室名称分组（处理同名科室）
        Map<String, List<Department>> nameGroupMap = new HashMap<>();
        for (Department dept : allDepts) {
            nameGroupMap.computeIfAbsent(dept.getDeptName(), k -> new ArrayList<>()).add(dept);
        }

        // 构建关联关系：如果有父科室，则同父科室下的子科室互相关联
        // 但由于 department 表没有 parent_id，这里用名称模糊匹配
        // 例如：呼吸内科 ↔ 内科，心血管内科 ↔ 内科
        for (Department dept : allDepts) {
            String deptName = dept.getDeptName();
            List<String> related = new ArrayList<>();

            // 如果科室名称包含"内科"，则关联所有包含"内科"的科室
            if (deptName.contains("内科")) {
                for (String name : allDepartmentNames) {
                    if (name.contains("内科") && !name.equals(deptName)) {
                        related.add(name);
                    }
                }
            }
            // 如果科室名称包含"外科"，则关联所有包含"外科"的科室
            if (deptName.contains("外科")) {
                for (String name : allDepartmentNames) {
                    if (name.contains("外科") && !name.equals(deptName)) {
                        related.add(name);
                    }
                }
            }
            // 如果科室名称包含"科"，则关联包含相同关键字的科室
            // 提取科室名称中的核心关键词
            String keyword = extractKeyword(deptName);
            if (StringUtils.hasText(keyword) && !keyword.equals(deptName)) {
                for (String name : allDepartmentNames) {
                    if (name.contains(keyword) && !name.equals(deptName)) {
                        related.add(name);
                    }
                }
            }

            if (!related.isEmpty()) {
                relationMap.put(deptName, related);
            }
        }

        return relationMap;
    }

    /**
     * 提取科室名称中的核心关键词
     */
    private String extractKeyword(String deptName) {
        // 去除"科"字，提取核心
        if (deptName.endsWith("科") && deptName.length() > 2) {
            return deptName.substring(0, deptName.length() - 1);
        }
        // 如果包含"科"，取"科"前面的部分
        if (deptName.contains("科")) {
            return deptName.substring(0, deptName.indexOf("科"));
        }
        return deptName;
    }

    /**
     * AI分析主诉（使用结构化输出）
     */
    private AiDepartmentRecommendationDto analyzeWithAI(String chiefComplaint, List<String> deptNames) {
        String userPrompt = buildUserPrompt(chiefComplaint, deptNames);

        try {
            // 使用结构化输出：直接映射为 AiDepartmentRecommendationDto 对象
            AiDepartmentRecommendationDto result = chatClient.prompt()
                    .system(consultationSystemPrompt)
                    .user(userPrompt)
                    .options(OpenAiChatOptions.builder()
                            .temperature(0.3)
                            .maxTokens(500)
                            .build())
                    .call()
                    .entity(AiDepartmentRecommendationDto.class);  // 结构化输出

            log.info("AI结构化输出成功：parsedDiagnosis={}, recommendedDepartment={}, reason={}, emergency={}",
                    result.getParsedDiagnosis(),
                    result.getRecommendedDepartment(),
                    result.getDepartmentReason(),
                    result.getEmergency());

            // 验证推荐科室是否在数据库中存在
            List<String> validDepts = getAllDepartmentNamesFromDB();
            String recommendedDept = result.getRecommendedDepartment();

            if (StringUtils.hasText(recommendedDept) && !validDepts.contains(recommendedDept)) {
                // 如果AI返回的科室不在数据库中，尝试模糊匹配
                String matchedDept = fuzzyMatchDepartment(recommendedDept, validDepts);
                if (matchedDept != null) {
                    result.setRecommendedDepartment(matchedDept);
                    log.info("科室模糊匹配成功：{} → {}", recommendedDept, matchedDept);
                } else {
                    // 匹配失败，使用默认科室
                    String defaultDept = getDefaultDepartment();
                    result.setRecommendedDepartment(defaultDept);
                    log.warn("科室匹配失败，使用默认科室：{}", defaultDept);
                }
            }

            return result;

        } catch (Exception e) {
            log.error("AI结构化输出失败", e);
            // 降级处理：返回默认科室
            return AiDepartmentRecommendationDto.builder()
                    .parsedDiagnosis("根据您的描述，建议进一步就医检查")
                    .recommendedDepartment(getDefaultDepartment())
                    .departmentReason("AI分析异常，请重新描述症状或选择其他科室")
                    .emergency(false)
                    .build();
        }
    }

    /**
     * 获取默认科室（从数据库取第一个启用的科室）
     */
    private String getDefaultDepartment() {
        List<String> deptNames = getAllDepartmentNamesFromDB();
        return deptNames.isEmpty() ? "全科" : deptNames.get(0);
    }

    /**
     * 构建用户提示词
     */
    private String buildUserPrompt(String chiefComplaint, List<String> deptNames) {
        String deptList = String.join("、", deptNames);

        return String.format("""
                【患者主诉】
                %s
                
                【系统可用科室列表】
                %s
                
                【任务要求】
                1. 分析患者主诉，提取关键症状
                2. 从科室列表中选择最匹配的科室（必须从上述列表中选择）
                3. 给出推荐该科室的理由（30字以内）
                4. 对症状进行简要总结（20字以内）
                5. 如果主诉包含紧急症状（胸痛、呼吸困难、大出血等），设置 emergency 为 true
                
                【返回格式要求】
                必须返回JSON格式，包含以下字段（字段名必须完全一致）：
                {
                    "parsed_diagnosis": "症状总结（20字以内）",
                    "recommended_department": "科室名称（必须从科室列表中选择）",
                    "department_reason": "推荐理由（30字以内）",
                    "emergency": true或false
                }
                
                只返回JSON，不要返回其他任何内容。
                """,
                chiefComplaint,
                deptList
        );
    }

    /**
     * 模糊匹配科室名称
     */
    private String fuzzyMatchDepartment(String aiDept, List<String> validDepts) {
        if (!StringUtils.hasText(aiDept) || validDepts == null || validDepts.isEmpty()) {
            return null;
        }

        // 精确匹配
        if (validDepts.contains(aiDept)) {
            return aiDept;
        }

        // 包含匹配
        for (String validDept : validDepts) {
            if (validDept.contains(aiDept) || aiDept.contains(validDept)) {
                return validDept;
            }
        }

        // 关键词匹配
        String keyword = extractKeyword(aiDept);
        if (StringUtils.hasText(keyword)) {
            for (String validDept : validDepts) {
                if (validDept.contains(keyword)) {
                    return validDept;
                }
            }
        }

        return null;
    }

    /**
     * 计算医生匹配分值
     */
    private List<RecommendDoctorVo> calculateMatchScores(List<Doctor> doctors, String recommendedDept, String chiefComplaint) {
        if (doctors == null || doctors.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取科室名称映射
        Map<String, String> deptIdToName = departmentMapper.getDeptIdToNameMap();

        List<RecommendDoctorVo> result = new ArrayList<>();

        for (Doctor doctor : doctors) {
            // 只推荐启用且未删除的医生
            if (doctor.getStatus() == null || doctor.getStatus() != 1) {
                continue;
            }
            if (doctor.getIsDeleted() != null && doctor.getIsDeleted() == 1) {
                continue;
            }

            // 计算匹配分值
            BigDecimal matchScore = calculateDoctorMatchScore(doctor, recommendedDept, chiefComplaint);

            // 分值大于0才推荐
            if (matchScore.compareTo(BigDecimal.ZERO) > 0) {
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
        }

        // 按匹配分值降序排序
        return result.stream()
                .sorted((a, b) -> b.getMatchScore().compareTo(a.getMatchScore()))
                .collect(Collectors.toList());
    }

    /**
     * 计算单个医生的匹配分值
     */
    private BigDecimal calculateDoctorMatchScore(Doctor doctor, String recommendedDept, String chiefComplaint) {
        BigDecimal totalScore = BigDecimal.ZERO;

        // 1. 科室匹配度（40分）
        BigDecimal deptScore = calculateDepartmentMatch(doctor, recommendedDept);

        // 2. 职称/职位匹配（30分）
        BigDecimal positionScore = calculatePositionMatch(doctor);

        // 3. 擅长领域匹配（20分）
        BigDecimal goodAtScore = calculateGoodAtMatch(doctor, chiefComplaint);

        // 4. 个人简介匹配（10分）
        BigDecimal introScore = calculateIntroductionMatch(doctor, chiefComplaint);

        totalScore = deptScore.add(positionScore).add(goodAtScore).add(introScore);

        return totalScore.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 科室匹配度计算（最高40分）
     */
    private BigDecimal calculateDepartmentMatch(Doctor doctor, String recommendedDept) {
        if (!StringUtils.hasText(recommendedDept) || !StringUtils.hasText(doctor.getDepartmentId())) {
            return BigDecimal.ZERO;
        }

        // 获取医生所在科室名称
        Department department = departmentMapper.selectByDeptId(doctor.getDepartmentId());
        if (department == null) {
            return BigDecimal.ZERO;
        }

        String doctorDeptName = department.getDeptName();
        if (!StringUtils.hasText(doctorDeptName)) {
            return BigDecimal.ZERO;
        }

        // 精确匹配：40分
        if (doctorDeptName.equals(recommendedDept)) {
            return new BigDecimal("40");
        }

        // 包含匹配：30分
        if (doctorDeptName.contains(recommendedDept) || recommendedDept.contains(doctorDeptName)) {
            return new BigDecimal("30");
        }

        // 关联科室匹配：20分（从数据库构建的关联关系）
        Map<String, List<String>> relationMap = getDepartmentRelationMap();
        List<String> relatedDepts = relationMap.get(recommendedDept);
        if (relatedDepts != null && relatedDepts.contains(doctorDeptName)) {
            return new BigDecimal("20");
        }

        return BigDecimal.ZERO;
    }

    /**
     * 职称/职位匹配度（最高30分）
     */
    private BigDecimal calculatePositionMatch(Doctor doctor) {
        String position = doctor.getPosition();
        if (!StringUtils.hasText(position)) {
            return new BigDecimal("15");
        }

        for (Map.Entry<String, Integer> entry : POSITION_WEIGHT.entrySet()) {
            if (position.contains(entry.getKey())) {
                int weight = entry.getValue();
                return new BigDecimal(weight * 0.3);
            }
        }

        return new BigDecimal("15");
    }

    /**
     * 擅长领域匹配度（最高20分）
     */
    private BigDecimal calculateGoodAtMatch(Doctor doctor, String chiefComplaint) {
        String goodAt = doctor.getGoodAt();
        if (!StringUtils.hasText(goodAt) || !StringUtils.hasText(chiefComplaint)) {
            return BigDecimal.ZERO;
        }

        List<String> keywords = extractKeywords(chiefComplaint);

        int matchCount = 0;
        for (String keyword : keywords) {
            if (goodAt.contains(keyword)) {
                matchCount++;
            }
        }

        int score = Math.min(matchCount * 5, 20);
        return new BigDecimal(score);
    }

    /**
     * 个人简介匹配度（最高10分）
     */
    private BigDecimal calculateIntroductionMatch(Doctor doctor, String chiefComplaint) {
        String introduction = doctor.getIntroduction();
        if (!StringUtils.hasText(introduction) || !StringUtils.hasText(chiefComplaint)) {
            return BigDecimal.ZERO;
        }

        List<String> keywords = extractKeywords(chiefComplaint);

        int matchCount = 0;
        for (String keyword : keywords) {
            if (introduction.contains(keyword)) {
                matchCount++;
            }
        }

        int score = Math.min(matchCount * 2, 10);
        return new BigDecimal(score);
    }

    /**
     * 提取关键词
     */
    private List<String> extractKeywords(String text) {
        List<String> keywords = new ArrayList<>();

        String[] symptomKeywords = {"发热", "咳嗽", "头痛", "腹痛", "恶心", "呕吐", "腹泻", "便秘",
                "胸痛", "心悸", "气短", "头晕", "乏力", "皮疹", "瘙痒", "关节痛",
                "腰痛", "背痛", "失眠", "焦虑", "抑郁", "视力模糊", "耳鸣"};

        for (String keyword : symptomKeywords) {
            if (text.contains(keyword)) {
                keywords.add(keyword);
            }
        }

        return keywords;
    }

    /**
     * 构建匹配原因说明
     */
    private String buildMatchReason(Doctor doctor, BigDecimal matchScore) {
        List<String> reasons = new ArrayList<>();

        if (StringUtils.hasText(doctor.getPosition())) {
            reasons.add("职称：" + doctor.getPosition());
        }

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
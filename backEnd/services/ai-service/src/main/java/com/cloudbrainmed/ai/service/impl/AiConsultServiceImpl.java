package com.cloudbrainmed.ai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.cloudbrainmed.ai.dto.ConsultRecommendDto;
import com.cloudbrainmed.ai.entity.Doctor;
import com.cloudbrainmed.ai.entity.Department;
import com.cloudbrainmed.ai.mapper.DoctorMapper;
import com.cloudbrainmed.ai.mapper.DepartmentMapper;
import com.cloudbrainmed.ai.service.AiConsultService;
import com.cloudbrainmed.ai.vo.AiRecommendResponseVo;
import com.cloudbrainmed.ai.vo.RecommendDoctorVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AiConsultServiceImpl implements AiConsultService {

    private final ChatClient chatClient;
    private final DoctorMapper doctorMapper;
    private final DepartmentMapper departmentMapper;

    // 构造器注入
    public AiConsultServiceImpl(ChatClient.Builder builder,
                                DoctorMapper doctorMapper,
                                DepartmentMapper departmentMapper) {
        this.chatClient = builder.build();
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

    @Override
    public AiRecommendResponseVo recommendDoctor(ConsultRecommendDto consultRecommendDto) {
        String chiefComplaint = consultRecommendDto.getChiefComplaint();

        // 1. 从数据库获取所有科室名称，构建提示词
        List<String> deptNames = getAllDepartmentNamesFromDB();

        // 2. AI解析主诉，获取推荐科室（传入科室列表）
        AiAnalysisResult aiResult = analyzeWithAI(chiefComplaint, deptNames);

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
     * AI分析主诉
     */
    private AiAnalysisResult analyzeWithAI(String chiefComplaint, List<String> deptNames) {
        String prompt = buildAnalysisPrompt(chiefComplaint, deptNames);

        try {
            String aiResponse = chatClient.prompt(prompt)
                    .options(OpenAiChatOptions.builder()
                            .temperature(0.3)
                            .maxTokens(500)
                            .build())
                    .call()
                    .content();

            log.info("AI响应内容：{}", aiResponse);

            return parseAiResponse(aiResponse);
        } catch (Exception e) {
            log.error("AI调用失败", e);
            // 降级处理：返回默认科室
            return AiAnalysisResult.builder()
                    .parsedDiagnosis("根据您的描述，建议进一步就医检查")
                    .recommendedDepartment(getDefaultDepartment())
                    .departmentReason("AI分析异常，请重新描述症状或选择其他科室")
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
     * 构建AI提示词（包含数据库中的科室列表）
     */
    private String buildAnalysisPrompt(String chiefComplaint, List<String> deptNames) {
        // 构建科室列表字符串
        String deptList = String.join("、", deptNames);

        return String.format(
                "你是一位专业的医疗AI助手，请分析以下患者主诉，并严格按照JSON格式返回结果。\n\n" +
                        "患者主诉：%s\n\n" +
                        "系统支持的科室列表（请从以下科室中选择推荐）：%s\n\n" +
                        "请分析并返回以下JSON格式（不要返回其他内容）：\n" +
                        "{\n" +
                        "  \"parsed_diagnosis\": \"对症状的简要总结（20字以内）\",\n" +
                        "  \"recommended_department\": \"从科室列表中选择最匹配的一个科室名称\",\n" +
                        "  \"department_reason\": \"推荐该科室的原因（30字以内）\"\n" +
                        "}\n\n" +
                        "注意：推荐科室必须从上述科室列表中选择，只返回JSON格式。",
                chiefComplaint,
                deptList
        );
    }

    /**
     * 解析AI响应
     */
    private AiAnalysisResult parseAiResponse(String aiResponse) {
        try {
            // 提取JSON内容
            String jsonStr = extractJson(aiResponse);
            JSONObject json = JSON.parseObject(jsonStr);

            String recommendedDept = json.getString("recommended_department");

            // 验证推荐科室是否在数据库中存在
            List<String> validDepts = getAllDepartmentNamesFromDB();
            if (StringUtils.hasText(recommendedDept) && !validDepts.contains(recommendedDept)) {
                // 如果AI返回的科室不在数据库中，尝试模糊匹配
                recommendedDept = fuzzyMatchDepartment(recommendedDept, validDepts);
                if (recommendedDept == null) {
                    recommendedDept = getDefaultDepartment();
                }
            }

            return AiAnalysisResult.builder()
                    .parsedDiagnosis(json.getString("parsed_diagnosis"))
                    .recommendedDepartment(recommendedDept)
                    .departmentReason(json.getString("department_reason"))
                    .build();
        } catch (Exception e) {
            log.error("解析AI响应失败", e);
            return AiAnalysisResult.builder()
                    .parsedDiagnosis("AI分析完成，建议咨询专业医生")
                    .recommendedDepartment(getDefaultDepartment())
                    .departmentReason("建议结合具体情况选择科室")
                    .build();
        }
    }

    /**
     * 模糊匹配科室名称
     */
    private String fuzzyMatchDepartment(String aiDept, List<String> validDepts) {
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
     * 提取JSON字符串
     */
    private String extractJson(String text) {
        Pattern pattern = Pattern.compile("\\{[^{}]*\\}");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return text;
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

    /**
     * AI分析结果内部类
     */
    @lombok.Builder
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class AiAnalysisResult {
        private String parsedDiagnosis;
        private String recommendedDepartment;
        private String departmentReason;
    }
}

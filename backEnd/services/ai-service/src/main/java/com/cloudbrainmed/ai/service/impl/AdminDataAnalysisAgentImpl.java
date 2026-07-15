package com.cloudbrainmed.ai.service.impl;

import com.alibaba.fastjson.JSON;
import com.cloudbrainmed.ai.dto.AdminAnalysisRequest;
import com.cloudbrainmed.ai.service.AdminDataAnalysisAgent;
import com.cloudbrainmed.ai.tool.MedicalStatisticsTool;
import com.cloudbrainmed.ai.tool.PatientStatisticsTool;
import com.cloudbrainmed.ai.tool.PaymentStatisticsTool;
import com.cloudbrainmed.ai.tool.RegisterStatisticsTool;
import com.cloudbrainmed.ai.vo.AdminAnalysisResponse;
import com.cloudbrainmed.ai.vo.AdminStatisticsResult;
import com.cloudbrainmed.ai.vo.SummaryItem;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class AdminDataAnalysisAgentImpl implements AdminDataAnalysisAgent {

    private final PatientStatisticsTool patientStatisticsTool;
    private final RegisterStatisticsTool registerStatisticsTool;
    private final MedicalStatisticsTool medicalStatisticsTool;
    private final PaymentStatisticsTool paymentStatisticsTool;
    private final ChatLanguageModel chatLanguageModel;
    private final String modelName;

    public AdminDataAnalysisAgentImpl(
            PatientStatisticsTool patientStatisticsTool,
            RegisterStatisticsTool registerStatisticsTool,
            MedicalStatisticsTool medicalStatisticsTool,
            PaymentStatisticsTool paymentStatisticsTool,
            ChatLanguageModel chatLanguageModel,
            @Value("${spring.ai.openai.chat.options.model:deepseek-v4-flash}") String modelName) {
        this.patientStatisticsTool = patientStatisticsTool;
        this.registerStatisticsTool = registerStatisticsTool;
        this.medicalStatisticsTool = medicalStatisticsTool;
        this.paymentStatisticsTool = paymentStatisticsTool;
        this.chatLanguageModel = chatLanguageModel;
        this.modelName = modelName;
    }

    @Override
    public AdminAnalysisResponse analyze(AdminAnalysisRequest request) {
        validateRequest(request);
        LocalDate start = request.getStartTime();
        LocalDate end = request.getEndTime();
        AdminStatisticsResult statistics = collectStatistics(request.getQuestion(), start, end);
        if (statistics.getSummary().isEmpty() && statistics.getCharts().isEmpty()) {
            throw new IllegalStateException("当前条件下未查询到可分析的运营数据");
        }

        AdminAnalysisResponse response = new AdminAnalysisResponse();
        response.setSummary(statistics.getSummary());
        response.setCharts(statistics.getCharts());
        response.setAnalysis(generateAnalysis(request, statistics));
        return response;
    }

    private void validateRequest(AdminAnalysisRequest request) {
        if (request == null || !StringUtils.hasText(request.getQuestion())) {
            throw new IllegalArgumentException("分析问题不能为空");
        }
        LocalDate today = LocalDate.now();
        if (request.getEndTime() == null) {
            request.setEndTime(today);
        }
        if (request.getStartTime() == null) {
            request.setStartTime(request.getEndTime().minusDays(29));
        }
        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new IllegalArgumentException("统计开始时间不能晚于结束时间");
        }
    }

    private AdminStatisticsResult collectStatistics(String question, LocalDate start, LocalDate end) {
        String normalized = question.toLowerCase(Locale.ROOT);
        AdminStatisticsResult merged = new AdminStatisticsResult();
        boolean matched = false;

        if (containsAny(normalized, "患者", "病人", "性别", "年龄")) {
            merged.merge(patientStatisticsTool.execute(start, end));
            matched = true;
        }
        if (containsAny(normalized, "挂号", "接诊", "科室", "医生", "门诊", "运行")) {
            merged.merge(registerStatisticsTool.execute(start, end));
            matched = true;
        }
        if (containsAny(normalized, "检查", "检验", "报告", "exam", "lab", "异常", "医疗业务")) {
            merged.merge(medicalStatisticsTool.execute(start, end));
            matched = true;
        }
        if (containsAny(normalized, "收入", "支付", "缴费", "收费", "营收", "金额", "财务")) {
            merged.merge(paymentStatisticsTool.execute(start, end));
            matched = true;
        }
        if (!matched || containsAny(normalized, "医院", "整体", "综合", "运营", "概况", "运行")) {
            merged.merge(patientStatisticsTool.execute(start, end));
            merged.merge(registerStatisticsTool.execute(start, end));
            merged.merge(medicalStatisticsTool.execute(start, end));
            merged.merge(paymentStatisticsTool.execute(start, end));
        }
        return merged;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String generateAnalysis(AdminAnalysisRequest request, AdminStatisticsResult statistics) {
        try {
            String statsJson = JSON.toJSONString(statistics);
            PromptTemplate template = PromptTemplate.from("""
                    你是 CloudBrainMed 智能云脑诊疗系统的医院医疗运营数据统计分析智能体。
                    你必须严格遵守以下规则：
                    1. 只能基于【真实统计结果JSON】中的数据库统计结果进行分析。
                    2. 禁止编造、补算、猜测任何不存在的统计数字。
                    3. 如果某个指标不存在，只能说明本次统计结果未提供该指标。
                    4. 输出中文，面向医院管理员，语气专业、简洁。
                    5. 分析内容应包含：总体判断、关键指标解读、趋势或结构分析、运营建议。

                    管理员问题：{{question}}
                    统计时间：{{startTime}} 至 {{endTime}}
                    真实统计结果JSON：{{statistics}}

                    请生成运营分析文本。
                    """);
            Map<String, Object> variables = new HashMap<>();
            variables.put("question", request.getQuestion());
            variables.put("startTime", request.getStartTime().toString());
            variables.put("endTime", request.getEndTime().toString());
            variables.put("statistics", statsJson);
            String content = chatLanguageModel.generate(template.apply(variables).text());
            if (StringUtils.hasText(content)) {
                return content.trim();
            }
        } catch (Exception exception) {
            log.warn("LangChain4j管理员运营数据AI分析调用失败，返回规则化分析文本，模型：{}", modelName, exception);
        }
        return fallbackAnalysis(request, statistics);
    }

    private String fallbackAnalysis(AdminAnalysisRequest request, AdminStatisticsResult statistics) {
        StringBuilder builder = new StringBuilder();
        builder.append("已根据数据库真实业务数据完成").append(request.getStartTime())
                .append("至").append(request.getEndTime()).append("的运营统计。关键指标包括：");
        for (SummaryItem item : statistics.getSummary()) {
            builder.append(item.getTitle()).append("为").append(item.getValue()).append("；");
        }
        builder.append("前端可结合返回的charts字段查看趋势、排行和结构占比。LangChain4j AI分析服务暂不可用，建议管理员结合图表重点关注异常波动、高负载科室及收入变化情况。");
        return builder.toString();
    }
}

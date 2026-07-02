package com.cloudbrainmed.ai.service.impl;

import com.cloudbrainmed.ai.entity.AiInferenceLog;
import com.cloudbrainmed.ai.mapper.AiInferenceLogMapper;
import com.cloudbrainmed.ai.dto.ReportAnalysisDto;
import com.cloudbrainmed.ai.service.AiReportService;
import com.cloudbrainmed.ai.vo.ReportAnalysisVo;
import com.cloudbrainmed.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AiReportServiceImpl implements AiReportService {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;
    private final AiInferenceLogMapper inferenceLogMapper;

    public AiReportServiceImpl(ChatClient.Builder chatClientBuilder,
                               ObjectMapper objectMapper,
                               AiInferenceLogMapper inferenceLogMapper) {
        this.chatClientBuilder = chatClientBuilder;
        this.objectMapper = objectMapper;
        this.inferenceLogMapper = inferenceLogMapper;
    }

    @Override
    public ReportAnalysisVo analyze(ReportAnalysisDto dto) {
        if (dto == null || (!StringUtils.hasText(dto.getReportText())
                && (dto.getIndicators() == null || dto.getIndicators().isEmpty()))) {
            throw new BusinessException("请输入报告文本或结构化指标");
        }

        String reply = null;
        long startTime = System.currentTimeMillis();
        try {
            ChatClient chatClient = chatClientBuilder.build();
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(systemPrompt()));
            messages.add(new UserMessage(buildUserPrompt(dto)));
            reply = chatClient.prompt(new Prompt(messages)).call().content();
            ReportAnalysisVo vo = parseReply(reply);
            saveInferenceLog(dto, vo, "SUCCESS", System.currentTimeMillis() - startTime);
            return vo;
        } catch (Exception e) {
            ReportAnalysisVo vo = fallback(reply);
            saveInferenceLog(dto, vo, "FALLBACK", System.currentTimeMillis() - startTime);
            return vo;
        }
    }

    private String systemPrompt() {
        return """
                你是脑科临床检查/检验报告分析助手，只为医生提供辅助参考。
                请严格返回 JSON，不要输出 Markdown 或额外说明。JSON 结构如下：
                {
                  "summary": "一句话概括报告核心结论",
                  "riskLevel": "LOW/MEDIUM/HIGH",
                  "abnormalIndicators": [
                    {"name":"指标或影像发现", "value":"结果值", "referenceRange":"参考范围", "interpretation":"临床解释"}
                  ],
                  "suggestions": ["建议1", "建议2"],
                  "followUpAdvice": "复查或下一步处理建议"
                }
                要求：最多列出5个异常指标；无法判断时说明需要补充的信息；不得替代医生诊断。
                """;
    }

    private String buildUserPrompt(ReportAnalysisDto dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("报告类型：").append(defaultText(dto.getReportType(), "未注明")).append('\n');
        sb.append("挂号ID：").append(defaultText(dto.getRegisterId(), "未提供")).append('\n');
        if (StringUtils.hasText(dto.getReportText())) {
            sb.append("报告文本：\n").append(dto.getReportText()).append('\n');
        }
        if (dto.getIndicators() != null && !dto.getIndicators().isEmpty()) {
            sb.append("结构化指标：\n");
            for (ReportAnalysisDto.IndicatorDto indicator : dto.getIndicators()) {
                sb.append("- ")
                        .append(defaultText(indicator.getName(), "未命名"))
                        .append(": ")
                        .append(defaultText(indicator.getValue(), "--"))
                        .append(defaultText(indicator.getUnit(), ""))
                        .append("，参考范围 ")
                        .append(defaultText(indicator.getReferenceRange(), "--"))
                        .append("，标记 ")
                        .append(defaultText(indicator.getAbnormalFlag(), "未标记"))
                        .append('\n');
            }
        }
        return sb.toString();
    }

    private ReportAnalysisVo parseReply(String reply) throws Exception {
        String json = extractJson(reply);
        ReportAnalysisVo vo = objectMapper.readValue(json, ReportAnalysisVo.class);
        vo.setFallback(false);
        return vo;
    }

    private String extractJson(String reply) {
        if (reply == null) return "{}";
        String json = reply.trim();
        if (json.contains("```json")) {
            json = json.substring(json.indexOf("```json") + 7);
            if (json.contains("```")) json = json.substring(0, json.indexOf("```"));
        } else if (json.contains("```")) {
            json = json.substring(json.indexOf("```") + 3);
            if (json.contains("```")) json = json.substring(0, json.indexOf("```"));
        }
        return json.trim();
    }

    private ReportAnalysisVo fallback(String reply) {
        ReportAnalysisVo vo = new ReportAnalysisVo();
        vo.setFallback(true);
        vo.setRiskLevel("MEDIUM");
        vo.setSummary("报告分析暂未生成结构化结果，请医生结合原始报告判断。");
        vo.setSuggestions(List.of("核对报告原文和异常指标", "必要时补充病史、体征和既往检查结果"));
        vo.setFollowUpAdvice(reply == null ? "AI 服务暂不可用。" : reply.substring(0, Math.min(200, reply.length())));
        vo.setAbnormalIndicators(List.of());
        return vo;
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private void saveInferenceLog(ReportAnalysisDto dto, ReportAnalysisVo vo, String status, long durationMs) {
        try {
            AiInferenceLog log = new AiInferenceLog();
            log.setLogId("AIR" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            log.setTraceId(defaultText(dto.getRegisterId(), log.getLogId()));
            log.setCallSource("REPORT_ANALYSIS");
            log.setModelKey("clinical-report-analysis");
            log.setModelVersion("llm");
            log.setInputSummary(defaultText(dto.getReportType(), "UNKNOWN") + ": "
                    + abbreviate(dto.getReportText(), 200));
            log.setOutputSummary(defaultText(vo.getRiskLevel(), "UNKNOWN") + ": "
                    + abbreviate(vo.getSummary(), 200));
            log.setStatus(status);
            log.setDurationMs((int) Math.min(durationMs, Integer.MAX_VALUE));
            log.setCreatedAt(LocalDateTime.now());
            inferenceLogMapper.insert(log);
        } catch (Exception ignored) {
            // 审计日志失败不能阻断医生接诊流程。
        }
    }

    private String abbreviate(String text, int maxLength) {
        if (!StringUtils.hasText(text)) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }
}

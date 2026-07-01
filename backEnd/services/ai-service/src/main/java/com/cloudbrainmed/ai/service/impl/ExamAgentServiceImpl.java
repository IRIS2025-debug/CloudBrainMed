package com.cloudbrainmed.ai.service.impl;

import com.cloudbrainmed.ai.dto.ExamGenerateRequest;
import com.cloudbrainmed.ai.dto.ExamGenerateResponse;
import com.cloudbrainmed.ai.entity.AiInferenceLog;
import com.cloudbrainmed.ai.mapper.AiInferenceLogMapper;
import com.cloudbrainmed.ai.mapper.MedicalItemMapper;
import com.cloudbrainmed.ai.service.ExamAgentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ExamAgentServiceImpl implements ExamAgentService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final AiInferenceLogMapper aiInferenceLogMapper;
    private final VectorStore vectorStore;
    private final MedicalItemMapper medicalItemMapper;

    @Value("classpath:prompt/exam_agent.st")
    private Resource examAgentResource;
    private String examAgentPrompt;

    private static final String CALL_SOURCE = "AI_EXAM_AGENT";
    private static final Set<String> VALID_URGENCY = Set.of("NORMAL", "URGENT", "EMERGENCY");

    public ExamAgentServiceImpl(ChatClient chatClient,
                                ObjectMapper objectMapper,
                                AiInferenceLogMapper aiInferenceLogMapper,
                                VectorStore vectorStore,
                                MedicalItemMapper medicalItemMapper) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
        this.aiInferenceLogMapper = aiInferenceLogMapper;
        this.vectorStore = vectorStore;
        this.medicalItemMapper = medicalItemMapper;
    }

    @PostConstruct
    public void init() {
        try {
            this.examAgentPrompt = examAgentResource.getContentAsString(StandardCharsets.UTF_8);
            log.info("Exam Agent prompt loaded, length: {} chars", examAgentPrompt.length());
        } catch (IOException e) {
            log.error("Failed to load exam agent prompt, using default", e);
            this.examAgentPrompt = "你是一位临床检验科医师，请根据病历推荐检查项目。返回JSON格式。";
        }
    }

    @Override
    public ExamGenerateResponse generateExamSuggestions(ExamGenerateRequest request) {
        String traceId = "EXAM_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        long startTime = System.currentTimeMillis();
        StringBuilder reasoningTrace = new StringBuilder();

        try {
            // Step 1: Planner
            reasoningTrace.append("=== Step 1: Planner ===\n");
            ExamGenerateRequest.ExamGenerateContext ctx = request.getContext();
            boolean needRag = plannerNeedRag(ctx);
            reasoningTrace.append("Planner: ")
                    .append(needRag ? "需要检索知识库" : "无需检索知识库")
                    .append("\n\n");

            // Step 2: RAG Tool call
            String ragContext = "";
            if (needRag) {
                reasoningTrace.append("=== Step 2: RAG知识库检索 ===\n");
                ragContext = searchKnowledgeBase(ctx);
                reasoningTrace.append("RAG检索结果: ")
                        .append(ragContext.length())
                        .append(" 字符\n\n");
            } else {
                reasoningTrace.append("=== Step 2: RAG知识库检索 ===\n");
                reasoningTrace.append("跳过RAG\n\n");
            }

            // Step 3: LLM Reasoning
            reasoningTrace.append("=== Step 3: LLM推理 ===\n");
            String llmResponse = callLLM(ctx, ragContext, reasoningTrace);
            reasoningTrace.append("LLM输出长度: ").append(llmResponse.length()).append(" 字符\n\n");

            // Step 4: Formatter
            reasoningTrace.append("=== Step 4: Formatter ===\n");
            ExamGenerateResponse response = formatResponse(llmResponse, reasoningTrace);

            // Step 5: Trace
            reasoningTrace.append("=== Step 5: Trace记录 ===\n");
            long elapsed = System.currentTimeMillis() - startTime;
            reasoningTrace.append("总耗时: ").append(elapsed).append("ms\n");
            response.setReasoningTrace(reasoningTrace.toString());
            response.setTraceId(traceId);
            saveTraceLog(traceId, request, response, elapsed);

            return response;

        } catch (Exception e) {
            log.error("Exam Agent generation failed", e);
            return buildFallbackResponse(traceId, reasoningTrace.toString(), e.getMessage());
        }
    }

    private boolean plannerNeedRag(ExamGenerateRequest.ExamGenerateContext ctx) {
        String description = ctx.getDescription();
        if (description == null || description.trim().isEmpty()) {
            return false;
        }

        List<String> ragKeywords = Arrays.asList(
                "罕见病", "疑难", "复杂", "多系统", "鉴别诊断",
                "药物相互作用", "特殊人群", "妊娠", "儿童", "老年人",
                "传染病", "肿瘤", "自身免疫", "遗传"
        );

        String lowerDesc = description.toLowerCase();
        for (String keyword : ragKeywords) {
            if (lowerDesc.contains(keyword)) {
                return true;
            }
        }

        return description.length() > 200;
    }

    private String searchKnowledgeBase(ExamGenerateRequest.ExamGenerateContext ctx) {
        try {
            String query = buildSearchQuery(ctx);
            List<Document> docs = vectorStore.similaritySearch(query);
            if (docs == null || docs.isEmpty()) {
                return "";
            }
            return docs.stream()
                    .limit(3)
                    .map(Document::getText)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining("\n---\n"));
        } catch (Exception e) {
            log.warn("RAG search failed, continuing without knowledge base", e);
            return "";
        }
    }

    private String callLLM(ExamGenerateRequest.ExamGenerateContext ctx,
                           String ragContext,
                           StringBuilder reasoningTrace) {
        String userPrompt = buildUserPrompt(ctx, ragContext);
        reasoningTrace.append("用户提示词长度: ").append(userPrompt.length()).append(" 字符\n");

        String response = chatClient.prompt()
                .system(examAgentPrompt)
                .user(userPrompt)
                .call()
                .content();

        if (response == null || response.trim().isEmpty()) {
            throw new RuntimeException("LLM returned empty response");
        }

        return cleanJsonResponse(response);
    }

    @SuppressWarnings("unchecked")
    private ExamGenerateResponse formatResponse(String llmResponse,
                                                 StringBuilder reasoningTrace) {
        try {
            Map<String, Object> rawMap = objectMapper.readValue(llmResponse, Map.class);
            ExamGenerateResponse response = new ExamGenerateResponse();

            response.setClinicalSummary(getStringField(rawMap, "clinicalSummary"));

            List<Map<String, Object>> rawItems = (List<Map<String, Object>>) rawMap.get("checkItems");
            if (rawItems != null && !rawItems.isEmpty()) {
                List<ExamGenerateResponse.CheckItem> checkItems = rawItems.stream()
                        .map(item -> {
                            String itemName = (String) item.get("itemName");
                            Boolean selected = item.get("selected") instanceof Boolean
                                    ? (Boolean) item.get("selected") : true;
                            return new ExamGenerateResponse.CheckItem(itemName, selected);
                        })
                        .collect(Collectors.toList());
                response.setCheckItems(checkItems);
            } else {
                response.setCheckItems(new ArrayList<>());
            }

            String urgency = getStringField(rawMap, "urgencyLevel");
            response.setUrgencyLevel(VALID_URGENCY.contains(urgency) ? urgency : "NORMAL");

            reasoningTrace.append("格式化完成: checkItems=")
                    .append(response.getCheckItems().size())
                    .append(", urgency=")
                    .append(response.getUrgencyLevel())
                    .append("\n");

            return response;

        } catch (JsonProcessingException e) {
            log.error("Failed to parse LLM response", e);
            throw new RuntimeException("AI返回格式异常，请重试", e);
        }
    }

    private void saveTraceLog(String traceId,
                               ExamGenerateRequest request,
                               ExamGenerateResponse response,
                               long elapsedMs) {
        try {
            AiInferenceLog logEntry = new AiInferenceLog();
            logEntry.setLogId("AILOG_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            logEntry.setTraceId(traceId);
            logEntry.setPatientId(request.getContext().getPatientId());
            logEntry.setCallSource(CALL_SOURCE);
            logEntry.setModelKey("exam-agent");
            logEntry.setModelVersion("1.0.0");
            logEntry.setInputSummary(buildInputSummary(request));
            logEntry.setOutputSummary(buildOutputSummary(response));
            logEntry.setStatus("SUCCESS");
            logEntry.setDurationMs((int) elapsedMs);
            logEntry.setCreatedAt(LocalDateTime.now());
            aiInferenceLogMapper.insert(logEntry);
            log.info("Trace saved: traceId={}, elapsed={}ms", traceId, elapsedMs);
        } catch (Exception e) {
            log.warn("Failed to save trace log", e);
        }
    }

    private String buildSearchQuery(ExamGenerateRequest.ExamGenerateContext ctx) {
        String desc = ctx.getDescription();
        if (desc == null || desc.trim().isEmpty()) {
            return "临床检查检验指南";
        }
        return desc.length() > 300 ? desc.substring(0, 300) : desc;
    }

    private String buildUserPrompt(ExamGenerateRequest.ExamGenerateContext ctx, String ragContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("请根据以下患者病历信息，生成检查检验建议：\n\n");
        sb.append("【患者信息】\n");
        sb.append("患者年龄: ").append(ctx.getVisitAge() != null ? ctx.getVisitAge() + "岁" : "未知").append("\n\n");
        sb.append("【病历内容】\n");
        sb.append(ctx.getDescription() != null ? ctx.getDescription() : "无").append("\n");

        if (StringUtils.hasText(ragContext)) {
            sb.append("\n【知识库参考】\n");
            sb.append(ragContext).append("\n");
        }

        sb.append("\n【可用的检查检验项目列表（必须从以下项目中选择）】\n");
        List<Map<String, Object>> items = medicalItemMapper.selectEnabled();
        for (Map<String, Object> item : items) {
            String itemName = (String) item.get("item_name");
            String itemCategory = (String) item.get("item_category");
            String categoryLabel = "EXAM".equals(itemCategory) ? "检查" : "检验";
            sb.append("- ").append(itemName).append(" (").append(categoryLabel).append(")\n");
        }

        sb.append("\n请严格按照JSON格式返回检查建议，itemName 必须与上述列表中的项目名称完全一致。");
        return sb.toString();
    }

    private String cleanJsonResponse(String raw) {
        if (raw == null) return null;
        raw = raw.trim();
        if (raw.startsWith("```json")) {
            raw = raw.substring(7);
        } else if (raw.startsWith("```")) {
            raw = raw.substring(3);
        }
        if (raw.endsWith("```")) {
            raw = raw.substring(0, raw.length() - 3);
        }
        return raw.trim();
    }

    private String getStringField(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    private String buildInputSummary(ExamGenerateRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            return request.getContext().getDescription();
        }
    }

    private String buildOutputSummary(ExamGenerateResponse response) {
        try {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("clinicalSummary", response.getClinicalSummary());
            summary.put("itemCount", response.getCheckItems() != null ? response.getCheckItems().size() : 0);
            summary.put("urgencyLevel", response.getUrgencyLevel());
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException e) {
            return response.getClinicalSummary();
        }
    }

    private ExamGenerateResponse buildFallbackResponse(String traceId,
                                                        String reasoningTrace,
                                                        String errorMsg) {
        ExamGenerateResponse fallback = new ExamGenerateResponse();
        fallback.setClinicalSummary("AI检查生成暂时不可用: " + errorMsg);
        fallback.setCheckItems(new ArrayList<>());
        fallback.setUrgencyLevel("NORMAL");
        fallback.setReasoningTrace(reasoningTrace + "\nERROR: " + errorMsg);
        fallback.setTraceId(traceId);
        return fallback;
    }
}
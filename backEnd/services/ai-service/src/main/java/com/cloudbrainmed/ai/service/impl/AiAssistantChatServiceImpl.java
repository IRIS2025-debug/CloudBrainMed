package com.cloudbrainmed.ai.service.impl;

import com.cloudbrainmed.ai.dto.AiAssistantChatRequest;
import com.cloudbrainmed.ai.dto.AiAssistantChatResponse;
import com.cloudbrainmed.ai.dto.AiRecordGenerateRequest;
import com.cloudbrainmed.ai.dto.PrescriptionReviewRequest;
import com.cloudbrainmed.ai.entity.AiInferenceLog;
import com.cloudbrainmed.ai.enums.AiAssistantIntentEnum;
import com.cloudbrainmed.ai.mapper.AiInferenceLogMapper;
import com.cloudbrainmed.ai.service.AiAssistantChatService;
import com.cloudbrainmed.ai.service.AiMedicalRecordService;
import com.cloudbrainmed.ai.service.AiPrescriptionReviewService;
import com.cloudbrainmed.api.dto.ReportContextDto;
import com.cloudbrainmed.api.feign.DoctorFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI辅助接诊聊天服务实现。
 *
 * <p>本类负责医生AI对话框的核心编排：
 * 先读取doctor-service提供的可信患者上下文，再识别医生问题意图。
 * 问诊补全、信息缺失、上下文整理、诊断辅助由本类构造Prompt直接调用大模型；
 * 病历生成、处方审核则复用对应专业服务，避免重复实现专业能力。</p>
 */
@Service
public class AiAssistantChatServiceImpl implements AiAssistantChatService {

    private static final Logger log = LoggerFactory.getLogger(
            AiAssistantChatServiceImpl.class);
    private static final String CALL_SOURCE = "AI_ASSISTANT_CHAT";

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final DoctorFeignClient doctorFeignClient;
    private final AiInferenceLogMapper inferenceLogMapper;
    private final AiMedicalRecordService aiMedicalRecordService;
    private final AiPrescriptionReviewService aiPrescriptionReviewService;
    private final String modelName;
    private final String internalServiceKey;

    public AiAssistantChatServiceImpl(
            ChatClient.Builder chatClientBuilder,
            ObjectMapper objectMapper,
            DoctorFeignClient doctorFeignClient,
            AiInferenceLogMapper inferenceLogMapper,
            AiMedicalRecordService aiMedicalRecordService,
            AiPrescriptionReviewService aiPrescriptionReviewService,
            @Value("${spring.ai.openai.chat.options.model:deepseek-v4-flash}")
            String modelName,
            @Value("${internal.service-key:}") String internalServiceKey) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
        this.doctorFeignClient = doctorFeignClient;
        this.inferenceLogMapper = inferenceLogMapper;
        this.aiMedicalRecordService = aiMedicalRecordService;
        this.aiPrescriptionReviewService = aiPrescriptionReviewService;
        this.modelName = modelName;
        this.internalServiceKey = internalServiceKey;
    }

    @Override
    public AiAssistantChatResponse chat(
            AiAssistantChatRequest request, String doctorId) {
        // traceId贯穿本次聊天请求、专业模块调用结果和推理日志。
        String traceId = "AI" + compactUuid();
        long startedAt = System.currentTimeMillis();
        ReportContextDto context = getContext(request, doctorId);
        AiAssistantIntentEnum intent = resolveIntent(request);

        // 不属于辅助接诊自身处理的意图，转交给已有专业AI模块。
        if (!intent.isAssistantHandled()) {
            AiAssistantChatResponse delegated = delegateToSpecializedModule(
                    traceId, intent, request, doctorId);
            saveInferenceLog(
                    traceId,
                    context.getPatientId(),
                    summarizeInput(request, intent),
                    toJson(delegated),
                    delegated.getModuleResult() == null
                            ? "UNSUPPORTED" : "DELEGATED",
                    elapsed(startedAt));
            return delegated;
        }

        try {
            // 辅助接诊自身处理的意图统一通过聊天模型生成可展示文本。
            String answer = chatClient.prompt(new Prompt(buildMessages(
                    request, context, intent))).call().content();
            AiAssistantChatResponse response = new AiAssistantChatResponse();
            response.setTraceId(traceId);
            response.setIntent(intent.name());
            response.setAnswer(normalizeAnswer(answer));
            response.setModelVersion(modelName);
            response.setHandledByAssistant(true);
            response.setFallback(false);
            saveInferenceLog(
                    traceId,
                    context.getPatientId(),
                    summarizeInput(request, intent),
                    toJson(response),
                    "SUCCESS",
                    elapsed(startedAt));
            return response;
        } catch (Exception exception) {
            // 模型异常不能阻断医生接诊，返回可展示的降级提示并写失败日志。
            AiAssistantChatResponse fallback = new AiAssistantChatResponse();
            fallback.setTraceId(traceId);
            fallback.setIntent(intent.name());
            fallback.setAnswer("AI辅助接诊暂不可用，请继续根据患者主诉、现病史和既往资料手工完成问诊。");
            fallback.setModelVersion(modelName);
            fallback.setHandledByAssistant(true);
            fallback.setFallback(true);
            saveInferenceLog(
                    traceId,
                    context.getPatientId(),
                    summarizeInput(request, intent),
                    toJson(Map.of(
                            "errorType",
                            exception.getClass().getSimpleName())),
                    "FAILED",
                    elapsed(startedAt));
            return fallback;
        }
    }

    private ReportContextDto getContext(
            AiAssistantChatRequest request, String doctorId) {
        // 患者基本信息、主诉、历史病历和历史报告必须从doctor-service可信接口读取。
        ReportContextDto context = doctorFeignClient.getConsultContext(
                request.getRegisterId(), doctorId, requireInternalServiceKey());
        if (context == null || !context.isAvailable()) {
            throw new IllegalStateException(context == null
                    ? "无法获取患者接诊信息"
                    : context.getErrorMessage());
        }
        if (hasText(request.getCurrentRecordDesc())) {
            // 前端当前编辑内容可能尚未保存，因此优先使用请求中的草稿。
            context.setCurrentRecordDesc(request.getCurrentRecordDesc().trim());
        }
        return context;
    }

    /**
     * 识别医生问题意图。
     *
     * <p>快捷按钮传入的actionType优先级最高；自由文本则用关键词做保守匹配。
     * 未命中的问题作为CONTEXT_QA，由辅助接诊模块基于当前上下文回答。</p>
     */
    private AiAssistantIntentEnum resolveIntent(AiAssistantChatRequest request) {
        AiAssistantIntentEnum actionIntent =
                AiAssistantIntentEnum.fromActionType(request.getActionType());
        if (actionIntent != AiAssistantIntentEnum.UNKNOWN) {
            return actionIntent;
        }

        String message = request.getMessage() == null
                ? "" : request.getMessage().trim().toLowerCase();
        if (containsAny(message, "病历", "病历草稿", "现病史", "主诉病史")) {
            return AiAssistantIntentEnum.MEDICAL_RECORD_DRAFT;
        }
        if (containsAny(message, "用药", "处方", "药物", "药品", "青霉素",
                "过敏", "禁忌", "相互作用")) {
            return AiAssistantIntentEnum.PRESCRIPTION_REVIEW;
        }
        if (containsAny(message, "什么病", "可能是什么", "可能是",
                "诊断", "鉴别诊断", "诊断依据")) {
            return AiAssistantIntentEnum.DIAGNOSIS_ASSISTANT;
        }
        if (containsAny(message, "问哪些", "还需要问", "追问",
                "问诊建议", "问诊重点", "继续问")) {
            return AiAssistantIntentEnum.FOLLOW_UP_QUESTION;
        }
        if (containsAny(message, "缺哪些", "缺少", "不完整",
                "补充哪些", "待确认")) {
            return AiAssistantIntentEnum.MISSING_INFORMATION;
        }
        if (containsAny(message, "整理", "总结", "已知信息",
                "历史资料", "注意信息", "上下文")) {
            return AiAssistantIntentEnum.CONTEXT_SUMMARY;
        }
        return AiAssistantIntentEnum.CONTEXT_QA;
    }

    /**
     * 将病历、处方类意图委派给已有专业AI模块。
     *
     * <p>该方法只做参数转换和结果包装，不在辅助接诊模块中重新实现专业逻辑。</p>
     */
    private AiAssistantChatResponse delegateToSpecializedModule(
            String traceId,
            AiAssistantIntentEnum intent,
            AiAssistantChatRequest request,
            String doctorId) {
        AiAssistantChatResponse response = new AiAssistantChatResponse();
        response.setTraceId(traceId);
        response.setIntent(intent.name());
        response.setModelVersion(modelName);
        response.setHandledByAssistant(false);
        response.setFallback(false);
        try {
            switch (intent) {
                case MEDICAL_RECORD_DRAFT -> {
                    response.setHandledModule("AI_MEDICAL_RECORD");
                    response.setModuleResult(aiMedicalRecordService.generate(
                            toRecordGenerateRequest(request), doctorId));
                    response.setAnswer("已调用AI病历自动生成模块生成病历草稿。");
                }
                case PRESCRIPTION_REVIEW -> {
                    response.setHandledModule("AI_PRESCRIPTION_REVIEW");
                    if (request.getMedicines() == null
                            || request.getMedicines().isEmpty()) {
                        response.setAnswer("调用AI处方审核模块需要提供待审核药品列表。");
                    } else {
                        response.setModuleResult(
                                aiPrescriptionReviewService.review(
                                        toPrescriptionReviewRequest(request),
                                        doctorId));
                        response.setAnswer("已调用AI处方审核模块完成用药风险审核。");
                    }
                }
                default -> response.setAnswer(unsupportedMessage(
                        intent, request.getMessage()));
            }
        } catch (Exception exception) {
            response.setFallback(true);
            response.setAnswer("调用对应AI专业模块失败，请直接使用专业功能入口重试。");
            response.setModuleResult(Map.of(
                    "errorType", exception.getClass().getSimpleName()));
        }
        return response;
    }

    /**
     * 未支持意图的兜底提示。
     */
    private String unsupportedMessage(
            AiAssistantIntentEnum intent, String message) {
        return switch (intent) {
            case MEDICAL_RECORD_DRAFT ->
                    "该请求不由AI辅助接诊模块处理。请直接调用AI病历自动生成接口 /api/ai/report/generate。";
            case PRESCRIPTION_REVIEW ->
                    "该请求不由AI辅助接诊模块处理。请直接调用AI处方审核接口 /api/ai/prescription/review。";
            default ->
                    "当前问题不属于AI辅助接诊模块可直接处理的范围：" + message;
        };
    }

    private AiRecordGenerateRequest toRecordGenerateRequest(
            AiAssistantChatRequest request) {
        AiRecordGenerateRequest generateRequest =
                new AiRecordGenerateRequest();
        generateRequest.setRegisterId(request.getRegisterId());
        generateRequest.setCurrentRecordDesc(request.getCurrentRecordDesc());
        generateRequest.setStructuredParameters(
                request.getStructuredParameters());
        String conversationText = firstText(
                request.getConversationText(),
                request.getSymptomDescription(),
                request.getMessage(),
                request.getCurrentRecordDesc());
        generateRequest.setConversationText(conversationText);
        return generateRequest;
    }

    private PrescriptionReviewRequest toPrescriptionReviewRequest(
            AiAssistantChatRequest request) {
        PrescriptionReviewRequest reviewRequest =
                new PrescriptionReviewRequest();
        reviewRequest.setRegisterId(request.getRegisterId());
        reviewRequest.setCurrentRecordDesc(request.getCurrentRecordDesc());
        reviewRequest.setPatientInformation(request.getPatientInformation());
        reviewRequest.setMedicines(request.getMedicines());
        return reviewRequest;
    }

    /**
     * 构造模型消息。
     *
     * <p>系统消息定义AI能力边界，用户消息只承载本次患者上下文和医生问题。
     * 患者输入、历史病历和报告均按数据处理，不允许改变系统约束。</p>
     */
    private List<Message> buildMessages(
            AiAssistantChatRequest request,
            ReportContextDto context,
            AiAssistantIntentEnum intent) {
        String systemPrompt = buildSystemPrompt(intent);

        String userPrompt = """
            意图：%s
            医生问题：%s

            患者年龄：%s
            患者性别：%s
            挂号主诉：%s

            医生当前病历草稿：
            %s

            已完成的追问与回答：
            %s

            历史病历：
            %s

            历史检查报告：
            %s
            """.formatted(
                intent.name(),
                valueOrUnknown(request.getMessage()),
                valueOrUnknown(context.getPatientAge()),
                valueOrUnknown(context.getPatientGender()),
                valueOrUnknown(context.getChiefComplaint()),
                valueOrUnknown(context.getCurrentRecordDesc()),
                joinAnswers(sanitizeFollowUpAnswers(
                        request.getFollowUpAnswers())),
                joinHistory(context.getMedicalHistory()),
                joinHistory(context.getPreviousReports()));

        return List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt));
    }

    /**
     * 根据意图选择系统Prompt。
     *
     * <p>诊断辅助允许输出可能诊断和鉴别诊断；普通问诊Prompt禁止输出
     * 诊断、病历、检查和处方内容，避免与专业模块混淆。</p>
     */
    private String buildSystemPrompt(AiAssistantIntentEnum intent) {
        if (intent == AiAssistantIntentEnum.DIAGNOSIS_ASSISTANT) {
            return """
                你是医生接诊过程中的AI诊断辅助工具，回答对象是执业医生。
                你可以基于当前患者上下文输出诊断结论、疑似诊断或鉴别诊断建议，但必须遵守：
                1. 只能使用输入中明确存在的信息，不得编造症状、体征、病史、检查结果或过敏史；
                2. 信息不足时明确写出“依据不足”以及需要补充的关键信息；
                3. 输出应包含：可能诊断、支持依据、反对或不足依据、需要排除的高风险情况、建议下一步确认问题；
                4. 不生成病历草稿、检查检验项目清单、处方、用药方案或用药风险审核结论；
                5. 结论供医生参考，最终诊断由医生结合查体和检查结果确认。
                回答应简洁、分点、可直接展示在医生对话框中。
                """;
        }
        return """
            你是医生接诊过程中的AI辅助问诊助手，只允许做三类事情：
            1. 根据当前患者上下文，提示医生还需要追问哪些信息；
            2. 指出当前问诊信息缺失或需要确认的内容；
            3. 整理当前患者上下文中的已知信息和待确认信息。

            禁止输出病历草稿、现病史成文、正式诊断、疑似诊断、鉴别诊断、检查检验项目清单、
            处方、用药方案、用药风险审核结论。遇到这类需求时，只说明应使用对应专业模块。
            不得编造患者没有提供的症状、体征、病史、检查结果、过敏史或治疗经过。
            回答应面向医生，简洁、分点、可执行。
            """;
    }

    private String summarizeInput(
            AiAssistantChatRequest request,
            AiAssistantIntentEnum intent) {
        return toJson(Map.of(
                "registerId", valueOrEmpty(request.getRegisterId()),
                "intent", intent.name(),
                "messageLength", lengthOf(request.getMessage()),
                "currentRecordLength", lengthOf(request.getCurrentRecordDesc()),
                "followUpAnswerCount", sizeOf(request.getFollowUpAnswers())));
    }

    /**
     * 保存AI聊天审计日志。
     *
     * <p>日志失败不影响医生接诊主流程；inputSummary只保存摘要，避免完整病历文本进入日志。</p>
     */
    private void saveInferenceLog(
            String traceId,
            String patientId,
            String inputSummary,
            String outputSummary,
            String status,
            int durationMs) {
        try {
            AiInferenceLog inferenceLog = new AiInferenceLog();
            inferenceLog.setLogId(
                    "LOG" + compactUuid().substring(0, 29));
            inferenceLog.setTraceId(traceId);
            inferenceLog.setCallSource(CALL_SOURCE);
            inferenceLog.setModelKey(modelName);
            inferenceLog.setModelVersion(modelName);
            inferenceLog.setInputSummary(inputSummary);
            inferenceLog.setOutputSummary(outputSummary);
            inferenceLog.setStatus(status);
            inferenceLog.setDurationMs(durationMs);
            inferenceLog.setCreatedAt(LocalDateTime.now());
            inferenceLog.setPatientId(patientId);
            inferenceLogMapper.insert(inferenceLog);
        } catch (Exception exception) {
            log.warn("保存AI辅助接诊聊天日志失败, traceId={}",
                    traceId, exception);
        }
    }

    private Map<String, String> sanitizeFollowUpAnswers(
            Map<String, String> answers) {
        Map<String, String> sanitized = new LinkedHashMap<>();
        if (answers == null) {
            return sanitized;
        }
        answers.forEach((question, answer) -> {
            if (sanitized.size() < 10
                    && hasText(question) && hasText(answer)) {
                sanitized.put(
                        truncate(question.trim(), 500),
                        truncate(answer.trim(), 2000));
            }
        });
        return sanitized;
    }

    private String joinAnswers(Map<String, String> answers) {
        if (answers == null || answers.isEmpty()) {
            return "无";
        }
        List<String> lines = new ArrayList<>();
        answers.forEach((question, answer) ->
                lines.add("问：" + question + "\n答：" + answer));
        return lines.isEmpty() ? "无" : String.join("\n", lines);
    }

    private String joinHistory(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "无";
        }
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (result.size() >= 5) {
                break;
            }
            if (hasText(value)) {
                result.add(truncate(value.trim(), 2000));
            }
        }
        return result.isEmpty() ? "无" : String.join("\n", result);
    }

    /**
     * 标准化模型回答，避免空回复或过长文本直接透传给前端。
     */
    private String normalizeAnswer(String answer) {
        if (!hasText(answer)) {
            return "当前上下文不足，请继续补充患者主诉、症状经过和相关病史。";
        }
        return truncate(answer.trim(), 4000);
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从多个候选文本中取第一个非空值，用于补齐专业模块必需的输入文本。
     */
    private String firstText(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String requireInternalServiceKey() {
        if (!hasText(internalServiceKey)) {
            throw new IllegalStateException(
                    "INTERNAL_SERVICE_KEY is not configured");
        }
        return internalServiceKey;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private Object valueOrUnknown(Object value) {
        return value == null || value.toString().isBlank() ? "未知" : value;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int lengthOf(String value) {
        return value == null ? 0 : value.length();
    }

    private int sizeOf(Map<?, ?> values) {
        return values == null ? 0 : values.size();
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength
                ? value : value.substring(0, maxLength);
    }

    private int elapsed(long startedAt) {
        return (int) Math.min(
                System.currentTimeMillis() - startedAt,
                Integer.MAX_VALUE);
    }

    private String compactUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}

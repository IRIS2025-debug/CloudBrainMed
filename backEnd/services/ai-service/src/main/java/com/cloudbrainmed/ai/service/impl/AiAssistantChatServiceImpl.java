package com.cloudbrainmed.ai.service.impl;

import com.cloudbrainmed.ai.dto.AiAssistantChatRequest;
import com.cloudbrainmed.ai.dto.AiAssistantChatResponse;
import com.cloudbrainmed.ai.dto.AiRecordGenerateRequest;
import com.cloudbrainmed.ai.dto.PrescriptionReviewMedicineRequest;
import com.cloudbrainmed.ai.dto.PrescriptionReviewRequest;
import com.cloudbrainmed.ai.entity.Medicine;
import com.cloudbrainmed.ai.enums.AiAssistantResponseStatusEnum;
import com.cloudbrainmed.ai.enums.AiAssistantIntentEnum;
import com.cloudbrainmed.ai.enums.AiHandledModuleEnum;
import com.cloudbrainmed.ai.mapper.MedicineMapper;
import com.cloudbrainmed.ai.service.AiAssistantChatService;
import com.cloudbrainmed.ai.service.AiMedicalRecordService;
import com.cloudbrainmed.ai.service.AiPrescriptionDraftService;
import com.cloudbrainmed.ai.service.AiPrescriptionReviewService;
import com.cloudbrainmed.api.dto.ReportContextDto;
import com.cloudbrainmed.api.feign.DoctorFeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI辅助接诊聊天服务实现。
 *
 * <p>本类负责医生统一AI聊天框的核心编排：
 * 前端通过actionType显式选择能力；辅助接诊、聊天和辅助诊断统一由
 * RECEPTION_ASSISTANT处理，并会结合患者接诊上下文、医生输入和药品知识库；
 * 病历生成和处方审核则委派给对应专业服务。</p>
 */
@Service
public class AiAssistantChatServiceImpl implements AiAssistantChatService {

    private static final Logger log = LoggerFactory.getLogger(
            AiAssistantChatServiceImpl.class);

    private final ChatClient chatClient;
    private final DoctorFeignClient doctorFeignClient;
    private final AiMedicalRecordService aiMedicalRecordService;
    private final AiPrescriptionDraftService aiPrescriptionDraftService;
    private final AiPrescriptionReviewService aiPrescriptionReviewService;
    private final MedicineMapper medicineMapper;
    private final VectorStore vectorStore;
    private final String modelName;
    private final String internalServiceKey;

    public AiAssistantChatServiceImpl(
            ChatClient.Builder chatClientBuilder,
            DoctorFeignClient doctorFeignClient,
            AiMedicalRecordService aiMedicalRecordService,
            AiPrescriptionDraftService aiPrescriptionDraftService,
            AiPrescriptionReviewService aiPrescriptionReviewService,
            MedicineMapper medicineMapper,
            VectorStore vectorStore,
            @Value("${spring.ai.openai.chat.options.model:deepseek-v4-flash}")
            String modelName,
            @Value("${internal.service-key:}") String internalServiceKey) {
        this.chatClient = chatClientBuilder.build();
        this.doctorFeignClient = doctorFeignClient;
        this.aiMedicalRecordService = aiMedicalRecordService;
        this.aiPrescriptionDraftService = aiPrescriptionDraftService;
        this.aiPrescriptionReviewService = aiPrescriptionReviewService;
        this.medicineMapper = medicineMapper;
        this.vectorStore = vectorStore;
        this.modelName = modelName;
        this.internalServiceKey = internalServiceKey;
    }

    @Override
    public AiAssistantChatResponse chat(
            AiAssistantChatRequest request, String doctorId) {
        AiAssistantIntentEnum intent = resolveSelectedAbility(request);

        if (!intent.isAssistantHandled()) {
            return delegateToSpecializedModule(intent, request, doctorId);
        }

        return handleReceptionAssistant(request, doctorId, intent);
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
     * 解析前端统一聊天框选择的AI能力。
     *
     * <p>后端不再根据医生文本关键词自动切换能力，避免“病历”“处方”等
     * 普通讨论误触发专业模块。未传或无法识别时默认走辅助接诊。</p>
     */
    private AiAssistantIntentEnum resolveSelectedAbility(
            AiAssistantChatRequest request) {
        AiAssistantIntentEnum selectedAbility =
                AiAssistantIntentEnum.fromActionType(request.getActionType());
        return selectedAbility == AiAssistantIntentEnum.UNKNOWN
                ? AiAssistantIntentEnum.RECEPTION_ASSISTANT
                : selectedAbility;
    }

    private AiAssistantChatResponse handleReceptionAssistant(
            AiAssistantChatRequest request,
            String doctorId,
            AiAssistantIntentEnum intent) {
        try {
            ReportContextDto context = getContext(request, doctorId);
            String answer = chatClient.prompt(new Prompt(buildMessages(
                    request, context, intent))).call().content();
            AiAssistantChatResponse response = new AiAssistantChatResponse();
            response.setIntent(intent.name());
            response.setAnswer(normalizeAnswer(answer));
            response.setStatus(AiAssistantResponseStatusEnum.SUCCESS.name());
            response.setModelVersion(modelName);
            response.setHandledByAssistant(true);
            response.setFallback(false);
            return response;
        } catch (Exception exception) {
            AiAssistantChatResponse fallback = new AiAssistantChatResponse();
            fallback.setIntent(intent.name());
            fallback.setAnswer("AI辅助接诊暂不可用，请继续根据患者主诉、现病史和既往资料手工完成问诊。");
            fallback.setStatus(AiAssistantResponseStatusEnum.FAILED.name());
            fallback.setModelVersion(modelName);
            fallback.setHandledByAssistant(true);
            fallback.setFallback(true);
            log.warn("AI辅助接诊聊天失败", exception);
            return fallback;
        }
    }

    /**
     * 将病历、处方类意图委派给已有专业AI模块。
     *
     * <p>该方法只做参数转换和结果包装，不在辅助接诊模块中重新实现专业逻辑。</p>
     */
    private AiAssistantChatResponse delegateToSpecializedModule(
            AiAssistantIntentEnum intent,
            AiAssistantChatRequest request,
            String doctorId) {
        AiAssistantChatResponse response = new AiAssistantChatResponse();
        response.setIntent(intent.name());
        response.setModelVersion(modelName);
        response.setHandledByAssistant(false);
        response.setFallback(false);
        try {
            switch (intent) {
                case MEDICAL_RECORD_DRAFT -> {
                    response.setHandledModule(
                            AiHandledModuleEnum.MEDICAL_RECORD.code());
                    var generated = aiMedicalRecordService.generate(
                            toRecordGenerateRequest(request), doctorId);
                    response.setModuleResult(generated);
                    response.setStatus(generated.isFallback()
                            ? AiAssistantResponseStatusEnum.FAILED.name()
                            : AiAssistantResponseStatusEnum.DELEGATED.name());
                    response.setAnswer("已调用AI病历自动生成模块生成病历草稿。");
                }
                case PRESCRIPTION_DRAFT -> {
                    response.setHandledModule(
                            AiHandledModuleEnum.PRESCRIPTION_DRAFT.code());
                    var draft = aiPrescriptionDraftService.generate(
                            request, doctorId);
                    response.setModuleResult(draft);
                    response.setStatus(resolveDelegatedStatus(
                            draft.getStatus(), draft.isFallback()));
                    response.setAnswer("已调用AI处方草稿模块生成处方草稿，请医生审核后再提交正式处方。");
                }
                case PRESCRIPTION_REVIEW -> {
                    response.setHandledModule(
                            AiHandledModuleEnum.PRESCRIPTION_REVIEW.code());
                    if (request.getMedicines() == null
                            || request.getMedicines().isEmpty()) {
                        response.setAnswer("调用AI处方审核模块需要提供待审核药品列表。");
                        response.setStatus(
                                AiAssistantResponseStatusEnum.NEEDS_INPUT.name());
                    } else {
                        var review = aiPrescriptionReviewService.review(
                                toPrescriptionReviewRequest(request),
                                doctorId);
                        response.setModuleResult(review);
                        response.setStatus(review.isFallback()
                                ? AiAssistantResponseStatusEnum.FAILED.name()
                                : AiAssistantResponseStatusEnum.DELEGATED.name());
                        response.setAnswer("已调用AI处方审核模块完成用药风险审核。");
                    }
                }
                default -> {
                    response.setAnswer("当前AI能力不支持通过统一聊天框委派处理。");
                    response.setStatus(
                            AiAssistantResponseStatusEnum.UNSUPPORTED.name());
                }
            }
        } catch (Exception exception) {
            response.setFallback(true);
            response.setStatus(AiAssistantResponseStatusEnum.FAILED.name());
            response.setAnswer("调用对应AI专业模块失败，请直接使用专业功能入口重试。");
            response.setModuleResult(Map.of(
                    "errorType", exception.getClass().getSimpleName()));
        }
        return response;
    }

    private String resolveDelegatedStatus(
            String moduleStatus, boolean fallback) {
        if (fallback) {
            return AiAssistantResponseStatusEnum.FAILED.name();
        }
        if (AiAssistantResponseStatusEnum.NEEDS_INPUT.name()
                .equals(moduleStatus)) {
            return AiAssistantResponseStatusEnum.NEEDS_INPUT.name();
        }
        return AiAssistantResponseStatusEnum.DELEGATED.name();
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
        String systemPrompt = buildSystemPrompt();
        String medicineKnowledge = buildMedicineKnowledge(request);

        String userPrompt = """
            当前AI能力：%s
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

            药品知识参考：
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
                joinHistory(context.getPreviousReports()),
                medicineKnowledge);

        return List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt));
    }

    /**
     * 构造辅助接诊系统Prompt。
     *
     * <p>辅助接诊、普通聊天和辅助诊断共用同一份可信上下文，因此统一在
     * RECEPTION_ASSISTANT能力中处理。</p>
     */
    private String buildSystemPrompt() {
        return """
            你是医生接诊过程中的AI辅助接诊助手，回答对象是执业医生。
            你可以基于当前患者上下文、医生提供的信息和药品知识参考帮助医生完成：
            1. 生成下一步追问问题；
            2. 判断当前问诊信息缺失或需要确认的内容；
            3. 整理已知信息、历史资料和待确认事项；
            4. 基于当前接诊上下文回答普通接诊问题；
            5. 当医生明确询问诊断、疑似诊断、鉴别诊断或诊断依据时，提供辅助诊断分析；
            6. 当医生询问药品用法、注意事项、禁忌或副作用时，结合药品知识参考回答。

            必须遵守：
            1. 只能使用输入中明确存在的信息，不得编造症状、体征、病史、检查结果、过敏史或治疗经过；
            2. 进行辅助诊断时必须说明可能诊断、支持依据、反对或不足依据、需排除的高风险情况；
            3. 信息不足时明确说明依据不足，并指出需要补充的关键信息；
            4. 可回答药品知识和用药注意事项，但不得直接生成处方、替医生决定具体治疗方案或替代药师审核；
            5. 遇到病历生成、处方审核等专业模块需求时，只说明应切换到对应AI能力按钮；
            6. 所有结论仅供医生参考，最终诊断由医生结合查体和检查结果确认。
            回答应面向医生，简洁、分点、可执行。
            """ + plainTextOutputInstruction();
    }

    private String plainTextOutputInstruction() {
        return """

            输出格式要求：
            只返回普通中文文本，不要使用 Markdown。
            不要输出 #、###、-、*、**、反引号、表格或代码块。
            如需分点，直接换行书写短句，不要添加项目符号或编号前缀。
            """;
    }

    private String buildMedicineKnowledge(AiAssistantChatRequest request) {
        List<String> sections = new ArrayList<>();

        List<Medicine> medicines = resolveReferencedMedicines(request);
        if (!medicines.isEmpty()) {
            sections.add("【药品数据库信息】\n" + medicines.stream()
                    .map(this::formatMedicine)
                    .collect(Collectors.joining("\n\n")));
        }

        String query = buildMedicineSearchQuery(request, medicines);
        if (StringUtils.hasText(query)) {
            String vectorContext = searchMedicineKnowledge(query);
            if (StringUtils.hasText(vectorContext)) {
                sections.add("【药品说明书/知识库检索】\n" + vectorContext);
            }
        }

        return sections.isEmpty() ? "无" : String.join("\n\n", sections);
    }

    private List<Medicine> resolveReferencedMedicines(
            AiAssistantChatRequest request) {
        List<Medicine> result = new ArrayList<>();
        Set<String> seenIds = new LinkedHashSet<>();

        addMedicineById(request.getMedicineId(), result, seenIds);
        if (request.getMedicines() != null) {
            for (PrescriptionReviewMedicineRequest medicineRequest
                    : request.getMedicines()) {
                addMedicineById(medicineRequest.getMedicineId(),
                        result, seenIds);
            }
        }

        if (result.isEmpty() && hasText(request.getMessage())) {
            try {
                Medicine medicine = medicineMapper.findByKeyword(
                        request.getMessage().trim());
                if (medicine != null && seenIds.add(
                        medicine.getMedicineId())) {
                    result.add(medicine);
                }
            } catch (Exception exception) {
                log.warn("药品关键词查询失败", exception);
            }
        }

        return result;
    }

    private void addMedicineById(
            String medicineId,
            List<Medicine> result,
            Set<String> seenIds) {
        if (!hasText(medicineId) || !seenIds.add(medicineId.trim())) {
            return;
        }
        try {
            Medicine medicine = medicineMapper.selectById(medicineId.trim());
            if (medicine != null) {
                result.add(medicine);
            }
        } catch (Exception exception) {
            log.warn("药品ID查询失败: {}", medicineId, exception);
        }
    }

    private String buildMedicineSearchQuery(
            AiAssistantChatRequest request,
            List<Medicine> medicines) {
        List<String> parts = new ArrayList<>();
        if (hasText(request.getMessage())) {
            parts.add(request.getMessage().trim());
        }
        for (Medicine medicine : medicines) {
            if (hasText(medicine.getName())) {
                parts.add(medicine.getName());
            }
        }
        return String.join(" ", parts);
    }

    private String searchMedicineKnowledge(String query) {
        try {
            List<Document> documents = vectorStore.similaritySearch(query);
            if (documents == null || documents.isEmpty()) {
                return "";
            }
            return documents.stream()
                    .limit(3)
                    .map(Document::getText)
                    .filter(StringUtils::hasText)
                    .map(value -> truncate(value.trim(), 2000))
                    .collect(Collectors.joining("\n---\n"));
        } catch (Exception exception) {
            log.warn("药品知识库检索失败", exception);
            return "";
        }
    }

    private String formatMedicine(Medicine medicine) {
        return """
            药品ID：%s
            药品名称：%s
            规格：%s
            用法用量：%s
            适应症：%s
            注意事项：%s
            """.formatted(
                valueOrUnknown(medicine.getMedicineId()),
                valueOrUnknown(medicine.getName()),
                valueOrUnknown(medicine.getSpec()),
                valueOrUnknown(medicine.getUsage()),
                valueOrUnknown(medicine.getIndication()),
                valueOrUnknown(medicine.getAttention())).trim();
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
        return truncate(stripMarkdownFormatting(answer).trim(), 4000);
    }

    private String stripMarkdownFormatting(String answer) {
        String normalized = answer.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("**", "")
                .replace("__", "")
                .replace("`", "");
        String[] lines = normalized.split("\n", -1);
        List<String> cleanedLines = new ArrayList<>();
        for (String line : lines) {
            String cleaned = line
                    .replaceFirst("^\\s{0,3}#{1,6}\\s*", "")
                    .replaceFirst("^\\s*>\\s*", "")
                    .replaceFirst("^\\s*[-*+]\\s+", "")
                    .replaceFirst("^\\s*\\d+[.)]\\s+", "")
                    .trim();
            cleanedLines.add(cleaned);
        }
        return String.join("\n", cleanedLines)
                .replaceAll("(?m)^\\s*[-*#]+\\s*$", "")
                .replaceAll("\\n{3,}", "\n\n");
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

    private Object valueOrUnknown(Object value) {
        return value == null || value.toString().isBlank() ? "未知" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength
                ? value : value.substring(0, maxLength);
    }

}

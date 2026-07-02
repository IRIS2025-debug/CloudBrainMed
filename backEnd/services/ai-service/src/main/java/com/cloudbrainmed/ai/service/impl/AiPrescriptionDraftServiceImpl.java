package com.cloudbrainmed.ai.service.impl;

import com.cloudbrainmed.ai.dto.AiAssistantChatRequest;
import com.cloudbrainmed.ai.dto.PrescriptionDraftMedicine;
import com.cloudbrainmed.ai.dto.PrescriptionDraftResponse;
import com.cloudbrainmed.ai.dto.PrescriptionReviewMedicineRequest;
import com.cloudbrainmed.ai.dto.PrescriptionReviewRequest;
import com.cloudbrainmed.ai.entity.Medicine;
import com.cloudbrainmed.ai.mapper.MedicineMapper;
import com.cloudbrainmed.ai.service.AiPrescriptionDraftService;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AiPrescriptionDraftServiceImpl
        implements AiPrescriptionDraftService {

    private static final Logger log = LoggerFactory.getLogger(
            AiPrescriptionDraftServiceImpl.class);
    private static final int MAX_CANDIDATE_MEDICINES = 100;
    private static final int MAX_DRAFT_MEDICINES = 10;
    private static final Set<String> RISK_LEVELS =
            Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL", "UNKNOWN");

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final DoctorFeignClient doctorFeignClient;
    private final MedicineMapper medicineMapper;
    private final AiPrescriptionReviewService aiPrescriptionReviewService;
    private final String modelName;
    private final String internalServiceKey;

    public AiPrescriptionDraftServiceImpl(
            ChatClient.Builder chatClientBuilder,
            ObjectMapper objectMapper,
            DoctorFeignClient doctorFeignClient,
            MedicineMapper medicineMapper,
            AiPrescriptionReviewService aiPrescriptionReviewService,
            @Value("${spring.ai.openai.chat.options.model:deepseek-v4-flash}")
            String modelName,
            @Value("${internal.service-key:}") String internalServiceKey) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
        this.doctorFeignClient = doctorFeignClient;
        this.medicineMapper = medicineMapper;
        this.aiPrescriptionReviewService = aiPrescriptionReviewService;
        this.modelName = modelName;
        this.internalServiceKey = internalServiceKey;
    }

    @Override
    public PrescriptionDraftResponse generate(
            AiAssistantChatRequest request, String doctorId) {
        try {
            ReportContextDto context = getContext(request, doctorId);
            List<Medicine> candidates = loadCandidateMedicines();
            if (candidates.isEmpty()) {
                PrescriptionDraftResponse response = baseResponse();
                response.setStatus("NEEDS_INPUT");
                response.setSummary("药品数据库暂无可用于生成处方草稿的候选药品。");
                response.getWarnings().add("请医生通过正式处方功能手工选择药品。");
                return response;
            }

            String reply = chatClient.prompt(new Prompt(buildMessages(
                    request, context, candidates))).call().content();
            PrescriptionDraftResponse response = parseReply(reply);
            normalizeResponse(response, candidates);
            attachReviewResult(response, request, doctorId);
            response.setStatus("SUCCESS");
            response.setModelVersion(modelName);
            response.setFallback(false);
            return response;
        } catch (Exception exception) {
            log.warn("AI prescription draft generation failed", exception);
            return fallbackResponse();
        }
    }

    private ReportContextDto getContext(
            AiAssistantChatRequest request, String doctorId) {
        ReportContextDto context = doctorFeignClient.getConsultContext(
                request.getRegisterId(), doctorId,
                requireInternalServiceKey());
        if (context == null || !context.isAvailable()) {
            throw new IllegalStateException(context == null
                    ? "无法获取患者接诊信息"
                    : context.getErrorMessage());
        }
        if (hasText(request.getCurrentRecordDesc())) {
            context.setCurrentRecordDesc(
                    request.getCurrentRecordDesc().trim());
        }
        return context;
    }

    private List<Medicine> loadCandidateMedicines() {
        List<Medicine> values = medicineMapper.selectAll();
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<Medicine> result = new ArrayList<>();
        Set<String> seenIds = new LinkedHashSet<>();
        for (Medicine medicine : values) {
            if (result.size() >= MAX_CANDIDATE_MEDICINES) {
                break;
            }
            if (medicine == null || !hasText(medicine.getMedicineId())
                    || !hasText(medicine.getName())
                    || !seenIds.add(medicine.getMedicineId().trim())) {
                continue;
            }
            result.add(medicine);
        }
        return result;
    }

    private List<Message> buildMessages(
            AiAssistantChatRequest request,
            ReportContextDto context,
            List<Medicine> candidates) {
        String systemPrompt = """
            你是给执业医生使用的AI处方草稿助手。
            你只能生成处方草稿，不能保存、提交、开立正式处方，也不能替代医生治疗决策。
            必须遵守：
            1. 只能从用户消息中的候选药品数据库列表选择药品，不得编造medicineId或药品名称。
            2. 如果诊断、过敏史、妊娠哺乳、肝肾功能、年龄体重、禁忌证等信息不足，应把缺失项写入missingInformation。
            3. 用法用量依据不足时不要自行补全高风险剂量，可给出warning或missingInformation。
            4. 不得生成麻醉、精神、毒性、放射性等特殊管制药品建议，除非候选数据和医生输入均明确支持。
            5. 所有结论仅供医生审核，正式处方必须由医生确认后提交。
            6. 只返回合法JSON，不要返回Markdown或额外说明。
            JSON结构：
            {
              "summary": "处方草稿摘要",
              "overallRiskLevel": "LOW|MEDIUM|HIGH|CRITICAL|UNKNOWN",
              "medicines": [
                {
                  "medicineId": "候选药品ID",
                  "usage": "建议用法用量",
                  "quantity": 1,
                  "reason": "选择该药品的依据",
                  "warnings": ["该药品相关风险或注意事项"]
                }
              ],
              "missingInformation": ["仍需补充的信息"],
              "warnings": ["整体风险提示"]
            }
            """;

        String userPrompt = """
            医生问题：
            %s

            患者年龄：%s
            患者性别：%s
            本次主诉：%s

            当前病历草稿：
            %s

            症状补充描述：
            %s

            医患对话原文：
            %s

            结构化问诊参数：
            %s

            已完成追问与回答：
            %s

            患者补充信息：
            %s

            数据库历史病历：
            %s

            数据库历史检查检验报告：
            %s

            候选药品数据库列表：
            %s
            """.formatted(
                valueOrUnknown(request.getMessage()),
                valueOrUnknown(context.getPatientAge()),
                valueOrUnknown(context.getPatientGender()),
                valueOrUnknown(context.getChiefComplaint()),
                valueOrUnknown(context.getCurrentRecordDesc()),
                valueOrUnknown(request.getSymptomDescription()),
                valueOrUnknown(request.getConversationText()),
                toJson(sanitizeMap(request.getStructuredParameters())),
                toJson(sanitizeMap(request.getFollowUpAnswers())),
                toJson(sanitizeMap(request.getPatientInformation())),
                joinHistory(context.getMedicalHistory()),
                joinHistory(context.getPreviousReports()),
                toJson(formatCandidateMedicines(candidates)));
        return List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt));
    }

    private List<Map<String, Object>> formatCandidateMedicines(
            List<Medicine> candidates) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Medicine medicine : candidates) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("medicineId", medicine.getMedicineId());
            data.put("medicineName", valueOrEmpty(medicine.getName()));
            data.put("spec", valueOrEmpty(medicine.getSpec()));
            data.put("referenceUsage", valueOrEmpty(medicine.getUsage()));
            data.put("indication", valueOrEmpty(medicine.getIndication()));
            data.put("attention", valueOrEmpty(medicine.getAttention()));
            data.put("stock", medicine.getStock());
            result.add(data);
        }
        return result;
    }

    private PrescriptionDraftResponse parseReply(String reply)
            throws Exception {
        if (!hasText(reply)) {
            throw new IllegalStateException("AI返回内容为空");
        }
        int start = reply.indexOf('{');
        int end = reply.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("AI返回的不是合法JSON对象");
        }
        return objectMapper.readValue(
                reply.substring(start, end + 1),
                PrescriptionDraftResponse.class);
    }

    private void normalizeResponse(
            PrescriptionDraftResponse response,
            List<Medicine> candidates) {
        Map<String, Medicine> candidateById = new HashMap<>();
        candidates.forEach(item -> candidateById.put(
                item.getMedicineId(), item));

        response.setSummary(truncate(
                valueOrEmpty(response.getSummary()).trim(), 1000));
        response.setOverallRiskLevel(normalizeRisk(
                response.getOverallRiskLevel()));
        response.setMissingInformation(limitStrings(
                response.getMissingInformation(), 20, 300));
        response.setWarnings(limitStrings(response.getWarnings(), 20, 500));

        List<PrescriptionDraftMedicine> normalized = new ArrayList<>();
        Set<String> seenIds = new LinkedHashSet<>();
        if (response.getMedicines() != null) {
            for (PrescriptionDraftMedicine item : response.getMedicines()) {
                if (normalized.size() >= MAX_DRAFT_MEDICINES || item == null
                        || !hasText(item.getMedicineId())) {
                    continue;
                }
                Medicine source = candidateById.get(
                        item.getMedicineId().trim());
                if (source == null || !seenIds.add(source.getMedicineId())) {
                    response.getWarnings().add("已忽略不在药品数据库候选列表中的药品。");
                    continue;
                }
                normalized.add(normalizeMedicine(item, source));
            }
        }
        response.setMedicines(normalized);

        if (normalized.isEmpty()
                && response.getMissingInformation().isEmpty()) {
            response.getMissingInformation().add(
                    "当前信息不足，无法生成可靠处方草稿。");
        }
    }

    private PrescriptionDraftMedicine normalizeMedicine(
            PrescriptionDraftMedicine item, Medicine source) {
        PrescriptionDraftMedicine result = new PrescriptionDraftMedicine();
        result.setMedicineId(source.getMedicineId());
        result.setMedicineName(source.getName());
        result.setSpec(valueOrEmpty(source.getSpec()));
        result.setReason(truncate(valueOrEmpty(item.getReason()), 500));
        result.setWarnings(limitStrings(item.getWarnings(), 10, 300));

        String usage = hasText(item.getUsage())
                ? item.getUsage().trim()
                : valueOrEmpty(source.getUsage()).trim();
        if (!hasText(usage)) {
            result.getWarnings().add("缺少明确用法用量，需要医生补充。");
        }
        result.setUsage(truncate(usage, 200));

        Integer quantity = item.getQuantity();
        if (quantity == null || quantity < 1) {
            quantity = 1;
            result.getWarnings().add("数量缺失，已临时按1处理，请医生确认。");
        } else if (quantity > 10000) {
            quantity = 10000;
            result.getWarnings().add("数量超过系统上限，已截断为10000，请医生确认。");
        }
        if (source.getStock() != null && quantity > source.getStock()) {
            result.getWarnings().add(
                    "建议数量超过当前库存：" + source.getStock());
        }
        result.setQuantity(quantity);
        return result;
    }

    private void attachReviewResult(
            PrescriptionDraftResponse response,
            AiAssistantChatRequest sourceRequest,
            String doctorId) {
        if (response.getMedicines() == null
                || response.getMedicines().isEmpty()) {
            return;
        }
        try {
            PrescriptionReviewRequest reviewRequest =
                    new PrescriptionReviewRequest();
            reviewRequest.setRegisterId(sourceRequest.getRegisterId());
            reviewRequest.setCurrentRecordDesc(
                    sourceRequest.getCurrentRecordDesc());
            reviewRequest.setPatientInformation(
                    sanitizeMap(sourceRequest.getPatientInformation()));
            List<PrescriptionReviewMedicineRequest> medicines =
                    new ArrayList<>();
            for (PrescriptionDraftMedicine draftMedicine
                    : response.getMedicines()) {
                PrescriptionReviewMedicineRequest reviewMedicine =
                        new PrescriptionReviewMedicineRequest();
                reviewMedicine.setMedicineId(draftMedicine.getMedicineId());
                reviewMedicine.setUsage(draftMedicine.getUsage());
                reviewMedicine.setQuantity(draftMedicine.getQuantity());
                medicines.add(reviewMedicine);
            }
            reviewRequest.setMedicines(medicines);
            response.setReviewResult(aiPrescriptionReviewService.review(
                    reviewRequest, doctorId));
        } catch (Exception exception) {
            response.getWarnings().add("处方草稿已生成，但自动处方审核失败，请单独审核。");
            log.warn("Prescription draft review failed", exception);
        }
    }

    private PrescriptionDraftResponse baseResponse() {
        PrescriptionDraftResponse response = new PrescriptionDraftResponse();
        response.setModelVersion(modelName);
        response.setOverallRiskLevel("UNKNOWN");
        response.setFallback(false);
        return response;
    }

    private PrescriptionDraftResponse fallbackResponse() {
        PrescriptionDraftResponse response = baseResponse();
        response.setStatus("FAILED");
        response.setSummary("AI处方草稿生成暂不可用。");
        response.setFallback(true);
        response.getWarnings().add("请医生根据患者资料和药品数据库手工完成处方。");
        return response;
    }

    private Map<String, String> sanitizeMap(Map<String, String> values) {
        Map<String, String> result = new LinkedHashMap<>();
        if (values == null) {
            return result;
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (result.size() >= 30 || !hasText(entry.getKey())
                    || !hasText(entry.getValue())) {
                continue;
            }
            result.put(
                    truncate(entry.getKey().trim(), 100),
                    truncate(entry.getValue().trim(), 1000));
        }
        return result;
    }

    private List<String> limitStrings(
            List<String> values, int maxCount, int maxLength) {
        List<String> result = new ArrayList<>();
        if (values == null) {
            return result;
        }
        for (String value : values) {
            if (result.size() >= maxCount) {
                break;
            }
            if (hasText(value)) {
                result.add(truncate(value.trim(), maxLength));
            }
        }
        return result;
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

    private String normalizeRisk(String value) {
        if (!hasText(value)) {
            return "UNKNOWN";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return RISK_LEVELS.contains(normalized) ? normalized : "UNKNOWN";
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

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength
                ? value : value.substring(0, maxLength);
    }
}

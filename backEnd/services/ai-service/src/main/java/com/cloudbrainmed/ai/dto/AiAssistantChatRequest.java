package com.cloudbrainmed.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI辅助接诊聊天请求。
 *
 * <p>该请求承载统一AI聊天框输入。前端通过actionType显式选择AI能力；
 * 辅助接诊会同时结合接诊上下文、医生问题和可选药品知识，病历生成、
 * 处方审核等专业模块所需字段也放在这里，便于统一入口编排调用。</p>
 */
@Data
public class AiAssistantChatRequest {

    /** 当前接诊挂号ID，用于拉取可信患者上下文。 */
    @NotBlank
    @Size(max = 32)
    private String registerId;

    /** 医生在AI对话框中输入的自然语言问题。 */
    @Size(max = 2000)
    private String message;

    /** 前端按钮选择的AI能力；为空时默认普通辅助接诊。 */
    @Size(max = 64)
    private String actionType;

    /** 可选药品ID，用于辅助接诊聊天中补充药品知识上下文。 */
    @Size(max = 32)
    private String medicineId;

    /** 医生当前正在编辑的病历草稿，优先于数据库中的旧草稿。 */
    @Size(max = 10000)
    private String currentRecordDesc;

    /** 患者症状补充描述，供问诊和病历生成参考。 */
    @Size(max = 5000)
    private String symptomDescription;

    /** 医患对话原文，主要供AI病历自动生成模块整理病历。 */
    @Size(max = 20000)
    private String conversationText;

    /** 结构化问诊参数，例如发病时间、疼痛部位、诱因等。 */
    @Size(max = 30)
    private Map<String, String> structuredParameters =
            new LinkedHashMap<>();

    /** 医生已完成的AI追问及患者回答。 */
    @Size(max = 10)
    private Map<String, String> followUpAnswers = new LinkedHashMap<>();

    /** 处方审核需要的补充患者信息，例如过敏史、妊娠状态等。 */
    @Size(max = 30)
    private Map<String, String> patientInformation =
            new LinkedHashMap<>();

    /** 处方审核场景下待审核的药品列表。 */
    @Valid
    @Size(max = 10)
    private List<PrescriptionReviewMedicineRequest> medicines =
            new ArrayList<>();

    @AssertTrue(message = "message和actionType至少提供一项")
    public boolean isIntentInputProvided() {
        return hasText(message) || hasText(actionType);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
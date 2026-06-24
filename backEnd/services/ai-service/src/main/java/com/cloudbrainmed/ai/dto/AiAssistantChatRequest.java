package com.cloudbrainmed.ai.dto;

import jakarta.validation.constraints.NotBlank;
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
 * <p>该请求既支持医生自然语言提问，也支持快捷按钮通过actionType指定意图。
 * 病历生成、处方审核等专业模块所需的上下文字段也放在这里，
 * 便于聊天入口在识别意图后直接编排调用专业服务。</p>
 */
@Data
public class AiAssistantChatRequest {

    /** 当前接诊挂号ID，用于拉取可信患者上下文。 */
    @NotBlank
    @Size(max = 32)
    private String registerId;

    /** 医生在AI对话框中输入的自然语言问题。 */
    @NotBlank
    @Size(max = 2000)
    private String message;

    /** 快捷按钮指定的意图；存在时优先于关键词识别。 */
    @Size(max = 64)
    private String actionType;

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
}

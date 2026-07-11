package com.cloudbrainmed.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 医生端AI药品查询请求。
 *
 * <p>医生端为无状态知识查询，只需病症/药品问题，不绑定患者、不保存历史。
 * 保留 sessionId 仅用于前端会话标识，后端不做持久化。</p>
 */
public class DoctorMedicineChatRequest {

    @NotBlank(message = "question不能为空")
    private String question;

    private String sessionId;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}

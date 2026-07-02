package com.cloudbrainmed.ai.dto;

/**
 * AI 对话历史记录。
 * <p>
 * 用于以强类型方式序列化/反序列化对话历史，避免直接序列化 Spring AI 的 Message 对象
 * 导致的 FastJSON 循环引用（$ref）和泛型不安全问题。
 */
public class ChatRecord {

    private String role;
    private String content;

    public ChatRecord() {
    }

    public ChatRecord(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
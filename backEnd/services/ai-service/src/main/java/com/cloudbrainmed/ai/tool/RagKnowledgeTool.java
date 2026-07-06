package com.cloudbrainmed.ai.tool;

import com.cloudbrainmed.ai.service.AiRagRetrievalService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class RagKnowledgeTool {

    private final AiRagRetrievalService ragRetrievalService;

    public RagKnowledgeTool(AiRagRetrievalService ragRetrievalService) {
        this.ragRetrievalService = ragRetrievalService;
    }

    @Tool(
            name = "retrieveMedicalKnowledge",
            description = "检索医学知识库，获取与患者症状相关的医学知识，参数为患者主诉文本"
    )
    public String retrieveMedicalKnowledge(String chiefComplaint) {
        return ragRetrievalService.retrieveKnowledge(chiefComplaint);
    }
}
package com.cloudbrainmed.ai.controller;

import com.cloudbrainmed.ai.dto.ExamGenerateRequest;
import com.cloudbrainmed.ai.dto.ExamGenerateResponse;
import com.cloudbrainmed.ai.service.ExamAgentService;
import com.cloudbrainmed.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI检查检验生成Agent", description = "基于病历自动生成检查检验建议的AI Agent")
@RestController
@RequestMapping("/ai-service/agent/exam")
@Validated
@Slf4j
public class ExamAgentController {

    @Autowired
    private ExamAgentService examAgentService;

    /**
     * AI检查检验生成
     * ========================
     * Agent流程：
     * 1. Planner - 判断是否需要知识库
     * 2. Tool调用 - RAG知识库搜索
     * 3. LLM推理 - 生成检查建议
     * 4. Formatter - 结构化输出
     * 5. Trace记录
     * ========================
     */
    @Operation(summary = "AI生成检查检验建议", description = "根据病历信息，通过Agent流程生成检查检验建议")
    @PostMapping("/generate")
    public Result<ExamGenerateResponse> generate(@Valid @RequestBody ExamGenerateRequest request) {
        log.info("AI检查检验生成请求: registerId={}, patientId={}",
                request.getRegisterId(),
                request.getContext().getPatientId());

        ExamGenerateResponse response = examAgentService.generateExamSuggestions(request);

        return Result.success(response);
    }
}
package com.cloudbrainmed.ai.controller;

import com.cloudbrainmed.ai.dto.ConsultRecommendDto;
import com.cloudbrainmed.ai.service.AiConsultService;
import com.cloudbrainmed.ai.vo.AiRecommendResponseVo;
import com.cloudbrainmed.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * AI智能问诊控制器
 * 基于患者主诉提供智能科室推荐和医生推荐服务
 */
@Tag(name = "AI智能问诊助手", description = "提供AI智能科室推荐和医生推荐服务")
@RestController
@RequestMapping("/api/ai/consult")
@Validated
@Slf4j
public class AiConsultController {

    @Autowired
    private AiConsultService aiConsultService;

    /**
     * AI问诊推荐
     * 根据患者描述的症状和病情，通过AI语义解析自动推荐就诊科室，
     * 并结合医生职称、擅长领域、个人简介等维度，计算匹配分值，
     * 按分值从高到低生成医生推荐排行榜
     *
     * @param consultRecommendDto 问诊请求参数，包含患者主诉和可选的患者ID
     * @return Result包装的AI推荐结果，包含解析结论、推荐科室和医生排行榜
     */
    @Operation(summary = "AI问诊推荐", description = "根据患者主诉，AI智能推荐科室和医生排行榜")
    @PostMapping("/recommend")
    public Result<AiRecommendResponseVo> getDoctorRecommendation(
            @Valid @RequestBody ConsultRecommendDto consultRecommendDto) {
        log.info("AI问诊请求，主诉：{}", consultRecommendDto.getChiefComplaint());

        AiRecommendResponseVo response = aiConsultService.recommendDoctor(consultRecommendDto);

        return Result.success(response);
    }

    /**
     * 获取推荐科室列表
     * <p>
     * 获取系统中所有可用的科室列表，用于前端展示或测试
     *
     * @return Result包装的科室名称列表
     */
    @Operation(summary = "获取科室列表", description = "获取系统中所有可用的科室列表")
    @GetMapping("/departments")
    public Result<List<String>> getDepartments() {
        return Result.success(aiConsultService.getAllDepartments());
    }
}

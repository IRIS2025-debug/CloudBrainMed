package com.cloudbrainmed.doctor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import jakarta.validation.Valid;

@Data
public class SaveReportRequestDto {
    @NotBlank(message = "检查项目ID不能为空")
    private String orderItemId;    // 对应 medical_order_item.order_item_id

    @NotBlank(message = "挂号ID不能为空")
    private String registerId;

    private String reportTitle;

    @Valid
    private ModelResultDto artifact;

    @Valid
    private ModelResultDto lesion;

    @Valid
    private ComprehensiveInfoDto comprehensive;

    @Valid
    private ImageUrlsDto images;

    private String reportDoctor;

    @Data
    public static class ModelResultDto {
        private String findings;
        private String diagnosis;
        private String advice;
        private String riskLevel;
        private Object rawResult;
    }

    @Data
    public static class ComprehensiveInfoDto {
        private String findings;
        private String diagnosis;
        private String advice;
    }

    @Data
    public static class ImageUrlsDto {
        private String artifact;
        private String lesion;
    }
}
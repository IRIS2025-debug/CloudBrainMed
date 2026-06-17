package com.cloudbrainmed.ai.dto;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Size;

@Data
public class ConsultRecommendDto {

    @NotBlank(message = "主诉不能为空")
    @Size(max = 500, message = "主诉不能超过500字")
    private String chiefComplaint;  // 患者主诉
}
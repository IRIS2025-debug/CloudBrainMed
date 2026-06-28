package com.cloudbrainmed.ai.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiScheduleGenerateResponse {

    private String traceId;
    private String status;
    private String modelVersion;
    private String summary;
    private List<AiScheduleItem> items = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private List<String> optimizationReasons = new ArrayList<>();
    private boolean fallback;
}

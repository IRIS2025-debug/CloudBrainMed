package com.cloudbrainmed.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
public class ExamGenerateResponse {
    private String traceId;
    private String clinicalSummary;
    private List<CheckItem> checkItems = new ArrayList<>();
    private String urgencyLevel;
    private String reasoningTrace;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckItem {
        private String itemName;
        private Boolean selected;
    }
}

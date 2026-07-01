package com.cloudbrainmed.ai.dto;

import lombok.Data;
import java.util.List;

@Data
public class ExamGenerateResponse {

    private String clinicalSummary;

    private List<CheckItem> checkItems;

    private String urgencyLevel;

    private String reasoningTrace;

    private String traceId;

    @Data
    public static class CheckItem {
        private String itemName;
        private Boolean selected;

        public CheckItem() {}

        public CheckItem(String itemName, Boolean selected) {
            this.itemName = itemName;
            this.selected = selected;
        }
    }
}
package com.cloudbrainmed.admin.dto;

import com.cloudbrainmed.admin.entity.SchedulePlan;
import com.cloudbrainmed.admin.entity.SchedulePlanItem;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SchedulePlanResponse {
    private SchedulePlan plan;
    private List<SchedulePlanItem> items;
    private List<String> warnings;
}

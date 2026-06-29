package com.cloudbrainmed.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiScheduleConflictCheckRequest {

    @Valid
    @NotEmpty
    @Size(max = 100)
    private List<AiScheduleItem> items = new ArrayList<>();
}

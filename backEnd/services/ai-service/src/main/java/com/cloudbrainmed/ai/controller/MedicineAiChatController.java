package com.cloudbrainmed.ai.controller;

import com.cloudbrainmed.ai.dto.MedicineChatRequest;
import com.cloudbrainmed.ai.service.MedicineAiChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai-service/ai/medicine")
public class MedicineAiChatController {

    private final MedicineAiChatService medicineAiChatService;

    public MedicineAiChatController(MedicineAiChatService medicineAiChatService) {
        this.medicineAiChatService = medicineAiChatService;
    }

    @PostMapping("/chat")
    public String chat(@Valid @RequestBody MedicineChatRequest request) {
        return medicineAiChatService.chat(request);
    }
}

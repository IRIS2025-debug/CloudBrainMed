package com.cloudbrainmed.ai.controller;

import com.cloudbrainmed.ai.dto.MedicineQueryDto;
import com.cloudbrainmed.ai.service.impl.AiMedicineServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/ai-service/medicine")
public class AiMedicineController {
    @Autowired
    private AiMedicineServiceImpl aiMedicineService;

    @PostMapping(path = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody MedicineQueryDto dto) {
        SseEmitter emitter = new SseEmitter(300000L);
        aiMedicineService.chatStream(dto, emitter);
        return emitter;
    }
}
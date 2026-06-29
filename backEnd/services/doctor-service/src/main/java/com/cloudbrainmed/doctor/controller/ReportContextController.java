package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.api.dto.ReportContextDto;
import com.cloudbrainmed.doctor.service.ReportContextService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/internal/doctor/consult")
public class ReportContextController {

    private final ReportContextService reportContextService;
    private final String internalServiceKey;

    public ReportContextController(
            ReportContextService reportContextService,
            @Value("${internal.service-key:}") String internalServiceKey) {
        this.reportContextService = reportContextService;
        this.internalServiceKey = internalServiceKey;
    }

    @GetMapping("/context")
    public ReportContextDto getContext(
            @RequestParam String registerId,
            @RequestHeader("X-Doctor-Id") String doctorId,
            @RequestHeader(
                    value = "X-Internal-Service-Key",
                    required = false) String serviceKey) {
        verifyInternalService(serviceKey);
        return reportContextService.getContext(registerId, doctorId);
    }

    private void verifyInternalService(String providedKey) {
        if (internalServiceKey.isBlank() || providedKey == null
                || !MessageDigest.isEqual(
                    internalServiceKey.getBytes(StandardCharsets.UTF_8),
                    providedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}

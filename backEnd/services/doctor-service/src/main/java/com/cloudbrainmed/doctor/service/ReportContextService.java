package com.cloudbrainmed.doctor.service;

import com.cloudbrainmed.api.dto.ReportContextDto;

public interface ReportContextService {

    ReportContextDto getContext(String registerId, String doctorId);
}

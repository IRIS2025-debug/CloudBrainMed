package com.cloudbrainmed.ai.service;

import com.cloudbrainmed.ai.dto.ReportAnalysisDto;
import com.cloudbrainmed.ai.vo.ReportAnalysisVo;

public interface AiReportService {
    ReportAnalysisVo analyze(ReportAnalysisDto dto);
}

package com.cloudbrainmed.ai.service;

import com.cloudbrainmed.ai.dto.AdminAnalysisRequest;
import com.cloudbrainmed.ai.vo.AdminAnalysisResponse;

public interface AdminDataAnalysisAgent {
    AdminAnalysisResponse analyze(AdminAnalysisRequest request);
}

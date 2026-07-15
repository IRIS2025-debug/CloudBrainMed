package com.cloudbrainmed.ai.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Configuration
public class LangChain4jConfig {

    @Bean
    public ChatLanguageModel adminAnalysisChatLanguageModel(
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            @Value("${spring.ai.openai.base-url:https://dashscope.aliyuncs.com/compatible-mode}") String baseUrl,
            @Value("${spring.ai.openai.chat.options.model:deepseek-v4-flash}") String modelName,
            @Value("${spring.ai.openai.chat.options.temperature:0.1}") Double temperature,
            @Value("${spring.ai.openai.chat.options.max-tokens:1024}") Integer maxTokens) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("请先配置 spring.ai.openai.api-key 或环境变量 DASHSCOPE_API_KEY");
        }
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(normalizeBaseUrl(baseUrl))
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (normalized.endsWith("/v1")) {
            return normalized;
        }
        return normalized + "/v1";
    }
}

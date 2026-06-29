package com.cloudbrainmed.ai.config;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class EmbeddingConfig {

    @Value("${spring.ai.embedding.api-key:${spring.ai.openai.api-key:}}")
    private String embeddingApiKey;

    @Value("${spring.ai.embedding.base-url:https://dashscope.aliyuncs.com/compatible-mode}")
    private String embeddingBaseUrl;

    @Value("${spring.ai.embedding.model:text-embedding-v4}")
    private String embeddingModel;

    @Value("${spring.ai.embedding.dimensions:1024}")
    private Integer embeddingDimensions;

    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        OpenAiApi embeddingApi = OpenAiApi.builder()
                .baseUrl(embeddingBaseUrl)
                .apiKey(embeddingApiKey)
                .build();

        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(embeddingModel)
                .dimensions(embeddingDimensions)
                .build();

        return new OpenAiEmbeddingModel(embeddingApi, MetadataMode.EMBED, options);
    }
}

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

    @Value("${spring.ai.embedding.api-key}")  // ← 换成自定义路径
    private String embeddingApiKey;

    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        OpenAiApi embeddingApi = OpenAiApi.builder()
                .baseUrl("https://api.siliconflow.cn")
                .apiKey(embeddingApiKey)
                .build();

        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model("BAAI/bge-large-zh-v1.5")
                .build();

        return new OpenAiEmbeddingModel(embeddingApi, MetadataMode.EMBED, options);
    }
}
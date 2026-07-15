package com.cloudbrainmed.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 为 Spring AI 单独配置 RestClient，设置足够长的超时时间。
 * 这个配置与 LangChain4j 的超时完全独立，互不干扰。
 */
@Configuration
public class SpringAiHttpConfig {

    @Value("${spring.ai.openai.timeout:300000}")  // 默认5分钟
    private long timeoutMillis;

    @Value("${spring.ai.openai.connect-timeout:30000}")  // 连接超时30秒
    private long connectTimeoutMillis;

    @Bean
    @Primary
    public RestClient.Builder springAiRestClientBuilder() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(connectTimeoutMillis))
                .withReadTimeout(Duration.ofMillis(timeoutMillis));

        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(settings);

        return RestClient.builder()
                .requestFactory(requestFactory);
    }
}
package com.toystorage.backend.config.ai;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class OpenAiClientConfig {

    private static final String DEFAULT_OPENAI_BASE_URL =
            "https://api.openai.com";

    private final AiProperties aiProperties;


    // =========================================================
    // OPENAI REST CLIENT
    //
    // Client này chỉ cấu hình base URL.
    //
    // Authorization Bearer token sẽ được thêm khi thực hiện
    // request trong OpenAiClientService.
    //
    // Lý do:
    //
    // - backend vẫn khởi động được khi AI_ENABLED=false
    // - không tạo Bearer header rỗng
    // - không hard-code API key
    // =========================================================

    @Bean(name = "openAiRestClient")
    public RestClient openAiRestClient() {

        String baseUrl =
                resolveBaseUrl();

        return RestClient
                .builder()
                .baseUrl(
                        baseUrl
                )
                .build();
    }


    // =========================================================
    // BASE URL
    // =========================================================

    private String resolveBaseUrl() {

        String configuredBaseUrl =
                aiProperties.getBaseUrl();


        if (
                configuredBaseUrl == null
                        || configuredBaseUrl.isBlank()
        ) {

            return DEFAULT_OPENAI_BASE_URL;
        }


        String normalizedBaseUrl =
                configuredBaseUrl.trim();


        // Bỏ dấu "/" cuối URL để tránh:
        //
        // https://api.openai.com//v1/responses
        //
        while (
                normalizedBaseUrl.endsWith("/")
                        && normalizedBaseUrl.length() > 1
        ) {

            normalizedBaseUrl =
                    normalizedBaseUrl.substring(
                            0,
                            normalizedBaseUrl.length() - 1
                    );
        }


        return normalizedBaseUrl;
    }
}
package com.toystorage.backend.config.ai;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /**
     * Bật / tắt chức năng AI.
     *
     * application.properties:
     *
     * app.ai.enabled=${AI_ENABLED:false}
     */
    private boolean enabled;


    /**
     * Base URL của AI provider.
     *
     * Ví dụ:
     *
     * https://api.openai.com
     */
    private String baseUrl;


    /**
     * API key.
     *
     * KHÔNG hard-code trong source.
     *
     * Giá trị được lấy từ:
     *
     * OPENAI_API_KEY
     */
    private String apiKey;


    /**
     * Model dùng cho AI Chat.
     *
     * Giá trị được lấy từ:
     *
     * OPENAI_MODEL
     */
    private String model;


    // =========================================================
    // HELPER
    // =========================================================

    public boolean hasApiKey() {

        return apiKey != null
                && !apiKey.isBlank();
    }


    public boolean hasModel() {

        return model != null
                && !model.isBlank();
    }


    public boolean isReady() {

        return enabled
                && hasApiKey()
                && hasModel();
    }
}
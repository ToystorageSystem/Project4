package com.toystorage.backend.services.ai;

import com.fasterxml.jackson.databind.JsonNode;

import com.toystorage.backend.config.ai.AiProperties;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenAiClientService {

    private final AiProperties aiProperties;

    @Qualifier("openAiRestClient")
    private final RestClient openAiRestClient;


    // =========================================================
    // GENERATE TEXT
    //
    // Gọi:
    //
    // POST /v1/responses
    //
    // Input:
    //
    // - system instruction
    // - user prompt
    //
    // Output:
    //
    // - text đầu tiên được trả về từ output_text
    // =========================================================

    public String generateText(

            String systemInstruction,

            String userPrompt
    ) {

        validateConfiguration();

        validatePrompt(
                userPrompt
        );


        Map<String, Object> requestBody =
                new LinkedHashMap<>();


        requestBody.put(
                "model",
                aiProperties.getModel()
        );


        if (
                systemInstruction != null
                        && !systemInstruction.isBlank()
        ) {

            requestBody.put(
                    "instructions",
                    systemInstruction.trim()
            );
        }


        requestBody.put(
                "input",
                userPrompt.trim()
        );


        // Không yêu cầu OpenAI lưu response
        // cho module analytics nội bộ.
        requestBody.put(
                "store",
                false
        );


        try {

            JsonNode response =
                    openAiRestClient
                            .post()

                            .uri(
                                    "/v1/responses"
                            )

                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer "
                                            + aiProperties.getApiKey()
                            )

                            .contentType(
                                    MediaType.APPLICATION_JSON
                            )

                            .body(
                                    requestBody
                            )

                            .retrieve()

                            .body(
                                    JsonNode.class
                            );


            return extractOutputText(
                    response
            );

        } catch (
                RestClientResponseException exception
        ) {

            // Không trả raw response ra ngoài vì có thể chứa
            // thông tin provider không cần thiết.

            throw new IllegalStateException(
                    "Không thể nhận phản hồi từ AI provider. "
                            + "HTTP status: "
                            + exception.getStatusCode().value()
            );

        } catch (
                Exception exception
        ) {

            throw new IllegalStateException(
                    "Không thể kết nối tới AI provider"
            );
        }
    }


    // =========================================================
    // EXTRACT OUTPUT TEXT
    //
    // Responses API:
    //
    // output[]
    //   -> message
    //      -> content[]
    //         -> output_text
    //            -> text
    // =========================================================

    private String extractOutputText(
            JsonNode response
    ) {

        if (
                response == null
                        || response.isNull()
        ) {

            throw new IllegalStateException(
                    "AI provider trả về response rỗng"
            );
        }


        // =====================================================
        // FALLBACK:
        //
        // Nếu provider trả trực tiếp output_text ở root.
        // =====================================================

        JsonNode directOutputText =
                response.get(
                        "output_text"
                );


        if (
                directOutputText != null
                        && directOutputText.isTextual()
                        && !directOutputText
                                .asText()
                                .isBlank()
        ) {

            return directOutputText
                    .asText()
                    .trim();
        }


        // =====================================================
        // STANDARD RESPONSES API OUTPUT
        // =====================================================

        JsonNode output =
                response.get(
                        "output"
                );


        if (
                output == null
                        || !output.isArray()
        ) {

            throw new IllegalStateException(
                    "AI provider không trả về output hợp lệ"
            );
        }


        for (
                JsonNode outputItem
                : output
        ) {

            if (
                    outputItem == null
                            || !outputItem.isObject()
            ) {

                continue;
            }


            String outputType =
                    outputItem
                            .path(
                                    "type"
                            )
                            .asText(
                                    ""
                            );


            if (
                    !"message".equals(
                            outputType
                    )
            ) {

                continue;
            }


            JsonNode content =
                    outputItem.get(
                            "content"
                    );


            if (
                    content == null
                            || !content.isArray()
            ) {

                continue;
            }


            for (
                    JsonNode contentItem
                    : content
            ) {

                if (
                        contentItem == null
                                || !contentItem.isObject()
                ) {

                    continue;
                }


                String contentType =
                        contentItem
                                .path(
                                        "type"
                                )
                                .asText(
                                        ""
                                );


                if (
                        !"output_text".equals(
                                contentType
                        )
                ) {

                    continue;
                }


                String text =
                        contentItem
                                .path(
                                        "text"
                                )
                                .asText(
                                        ""
                                );


                if (
                        !text.isBlank()
                ) {

                    return text.trim();
                }
            }
        }


        throw new IllegalStateException(
                "AI provider không trả về nội dung văn bản"
        );
    }


    // =========================================================
    // VALIDATE CONFIGURATION
    // =========================================================

    private void validateConfiguration() {

        if (
                !aiProperties.isEnabled()
        ) {

            throw new IllegalStateException(
                    "AI hiện đang bị tắt. "
                            + "Cần cấu hình AI_ENABLED=true"
            );
        }


        if (
                !aiProperties.hasApiKey()
        ) {

            throw new IllegalStateException(
                    "Chưa cấu hình OPENAI_API_KEY"
            );
        }


        if (
                !aiProperties.hasModel()
        ) {

            throw new IllegalStateException(
                    "Chưa cấu hình OPENAI_MODEL"
            );
        }
    }


    // =========================================================
    // VALIDATE PROMPT
    // =========================================================

    private void validatePrompt(
            String userPrompt
    ) {

        if (
                userPrompt == null
                        || userPrompt.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Nội dung gửi tới AI không được để trống"
            );
        }
    }
}
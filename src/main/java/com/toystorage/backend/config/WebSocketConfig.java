package com.toystorage.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Cấu hình WebSocket cho chức năng
 * theo dõi vị trí Delivery Staff realtime.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig
        implements WebSocketMessageBrokerConfigurer {


    // =====================================================
    // MESSAGE BROKER
    // =====================================================

    @Override
    public void configureMessageBroker(
            MessageBrokerRegistry registry
    ) {

        /*
         * Backend broadcast dữ liệu realtime tới client.
         *
         * Ví dụ:
         *
         * /topic/deliveries/15/location
         *
         * Warehouse / Store / Business có thể subscribe
         * topic này để nhận vị trí mới.
         */
        registry.enableSimpleBroker(
                "/topic"
        );


        /*
         * Prefix dành cho message client gửi tới server
         * thông qua STOMP.
         *
         * Hiện tracking của chúng ta gửi GPS bằng REST API,
         * nên chưa cần @MessageMapping.
         *
         * Nhưng giữ /app để sau này có thể mở rộng.
         */
        registry.setApplicationDestinationPrefixes(
                "/app"
        );
    }


    // =====================================================
    // WEBSOCKET ENDPOINT
    // =====================================================

    @Override
    public void registerStompEndpoints(
            StompEndpointRegistry registry
    ) {

        /*
         * Frontend kết nối tới:
         *
         * ws://localhost:8080/ws
         *
         * Production:
         *
         * wss://domain.com/ws
         */
        registry
                .addEndpoint("/ws")

                /*
                 * DEV:
                 * cho phép frontend từ origin khác kết nối.
                 *
                 * Production nên thay "*" bằng domain
                 * frontend thực tế.
                 */
                .setAllowedOriginPatterns("*");
    }
}
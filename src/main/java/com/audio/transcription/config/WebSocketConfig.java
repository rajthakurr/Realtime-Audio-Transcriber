package com.audio.transcription.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.audio.transcription.handler.AudioStreamWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * WebSocket configuration for bi-directional audio streaming.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private AudioStreamWebSocketHandler audioStreamWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(audioStreamWebSocketHandler, "/ws/transcribe")
                .setAllowedOrigins("http://localhost:3000", "http://localhost:8080")
                .withSockJS();
    }
}

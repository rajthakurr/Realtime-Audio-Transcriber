package com.audio.transcription.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Gemini API integration.
 */
@Data
@Component
@ConfigurationProperties(prefix = "gemini.api")
public class GeminiProperties {
    private String key;
    private String url;
    private String model;
    private int timeoutSeconds = 30;
    private int maxRetries = 3;
    private long retryDelayMs = 1000;
}

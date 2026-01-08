package com.audio.transcription.config;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.TimeUnit;

/**
 * Configuration for HTTP clients and REST templates.
 */
@Configuration
public class HttpClientConfig {

    /**
     * Configure OkHttpClient with connection pooling and timeouts.
     */
    @Bean
    public OkHttpClient okHttpClient(GeminiProperties geminiProperties) {
        return new OkHttpClient.Builder()
                .connectTimeout(geminiProperties.getTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(geminiProperties.getTimeoutSeconds(), TimeUnit.SECONDS)
                .writeTimeout(geminiProperties.getTimeoutSeconds(), TimeUnit.SECONDS)
                .callTimeout(geminiProperties.getTimeoutSeconds(), TimeUnit.SECONDS)
                .connectionPool(new okhttp3.ConnectionPool(20, 5, TimeUnit.MINUTES))
                .build();
    }

    /**
     * Configure RestTemplate for synchronous HTTP calls.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Configure WebClient for reactive HTTP calls.
     */
    @Bean
    public WebClient webClient(GeminiProperties geminiProperties) {
        return WebClient.builder()
                .build();
    }
}

package com.audio.transcription.service;

import com.audio.transcription.config.GeminiProperties;
import com.audio.transcription.dto.GeminiRequest;
import com.audio.transcription.dto.GeminiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Service for interfacing with Google Gemini API for audio transcription.
 * Handles streaming requests and real-time transcription responses.
 */
@Service
public class GeminiTranscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiTranscriptionService.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient okHttpClient;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public GeminiTranscriptionService(
            OkHttpClient okHttpClient,
            GeminiProperties geminiProperties,
            ObjectMapper objectMapper,
            CircuitBreaker geminiCircuitBreaker,
            Retry geminiRetry) {
        this.okHttpClient = okHttpClient;
        this.geminiProperties = geminiProperties;
        this.objectMapper = objectMapper;
        this.circuitBreaker = geminiCircuitBreaker;
        this.retry = geminiRetry;
    }

    /**
     * Send audio chunk to Gemini API for real-time transcription.
     * @param audioData Base64 encoded audio data
     * @param mimeType Audio MIME type (e.g., "audio/wav")
     * @return Mono containing transcription response
     */
    public Mono<GeminiResponse> transcribeAudioChunk(String audioData, String mimeType) {
        return Mono.fromSupplier(() -> {
            try {
                GeminiResponse response = sendAudioToGemini(audioData, mimeType);
                logger.debug("Transcription received from Gemini API");
                return response;
            } catch (IOException e) {
                logger.error("Failed to transcribe audio chunk", e);
                throw new RuntimeException("Transcription failed: " + e.getMessage(), e);
            }
        }).doOnError(error -> logger.error("Error in transcribeAudioChunk: {}", error.getMessage()));
    }

    /**
     * Send audio data to Gemini API with resilience patterns applied.
     */
    private GeminiResponse sendAudioToGemini(String audioData, String mimeType) throws IOException {
        // Create Gemini API request
        GeminiRequest geminiRequest = buildGeminiRequest(audioData, mimeType);

        // Wrap the actual call with Circuit Breaker and Retry
        Supplier<GeminiResponse> supplier = () -> {
            try {
                return executeGeminiRequest(geminiRequest);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        try {
            // Apply Retry and CircuitBreaker
            Supplier<GeminiResponse> retried = Retry.decorateSupplier(retry, supplier);
            Supplier<GeminiResponse> protected_ = CircuitBreaker.decorateSupplier(circuitBreaker, retried);
            return protected_.get();
        } catch (Exception e) {
            logger.error("All retry attempts failed or circuit breaker is open", e);
            throw new IOException("Failed to call Gemini API after retries", e);
        }
    }

    /**
     * Execute the actual HTTP request to Gemini API.
     */
    private GeminiResponse executeGeminiRequest(GeminiRequest geminiRequest) throws IOException {
        String requestBody = objectMapper.writeValueAsString(geminiRequest);
        logger.debug("Sending request to Gemini API: {} bytes", requestBody.length());

        String url = String.format("%s/%s:generateContent?key=%s",
                geminiProperties.getUrl(),
                geminiProperties.getModel(),
                geminiProperties.getKey());

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(requestBody, JSON))
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                logger.error("Gemini API error: {} - {}", response.code(), errorBody);
                throw new IOException("Gemini API returned " + response.code() + ": " + errorBody);
            }

            String responseBody = response.body().string();
            logger.debug("Received response from Gemini API: {} bytes", responseBody.length());
            return objectMapper.readValue(responseBody, GeminiResponse.class);
        }
    }

    /**
     * Build request payload for Gemini API.
     */
    private GeminiRequest buildGeminiRequest(String audioData, String mimeType) {
        GeminiRequest.InlineData inlineData = GeminiRequest.InlineData.builder()
                .mimeType(mimeType)
                .data(audioData)
                .build();

        GeminiRequest.Part part = GeminiRequest.Part.builder()
                .inlineData(inlineData)
                .build();

        GeminiRequest.Content content = GeminiRequest.Content.builder()
                .parts(List.of(part, 
                    GeminiRequest.Part.builder()
                        .text("Transcribe the audio precisely. Respond with only the transcribed text.")
                        .build()))
                .build();

        GeminiRequest.GenerationConfig config = GeminiRequest.GenerationConfig.builder()
                .temperature(0.3)
                .topP(0.95)
                .topK(40)
                .maxOutputTokens(1024)
                .build();

        return GeminiRequest.builder()
                .contents(List.of(content))
                .generationConfig(config)
                .build();
    }

    /**
     * Check health of Gemini API connection.
     */
    public String getApiKey() {
        return geminiProperties.getKey();
    }

    /**
     * Check health of Gemini API connection.
     */
    public Mono<Boolean> healthCheck() {
        return Mono.fromSupplier(() -> {
            try {
                String url = String.format("%s?key=%s",
                        geminiProperties.getUrl(),
                        geminiProperties.getKey());

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();

                try (Response response = okHttpClient.newCall(request).execute()) {
                    return response.isSuccessful();
                }
            } catch (Exception e) {
                logger.warn("Health check failed", e);
                return false;
            }
        });
    }
}

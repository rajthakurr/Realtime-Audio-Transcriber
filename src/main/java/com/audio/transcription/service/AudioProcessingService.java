package com.audio.transcription.service;

import com.audio.transcription.config.AudioProperties;
import com.audio.transcription.dto.AudioChunkRequest;
import com.audio.transcription.dto.GeminiResponse;
import com.audio.transcription.dto.TranscriptionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for processing audio chunks and coordinating transcription.
 * Handles buffering, real-time processing, and response streaming.
 */
@Service
public class AudioProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(AudioProcessingService.class);

    private final GeminiTranscriptionService geminiService;
    private final AudioProperties audioProperties;
    private final ConcurrentHashMap<String, SessionContext> sessionContexts = new ConcurrentHashMap<>();

    public AudioProcessingService(
            GeminiTranscriptionService geminiService,
            AudioProperties audioProperties) {
        this.geminiService = geminiService;
        this.audioProperties = audioProperties;
    }

    /**
     * Process incoming audio chunk and send for transcription.
     * @param request Audio chunk request
     * @return Mono containing transcription response
     */
    public Mono<TranscriptionResponse> processAudioChunk(AudioChunkRequest request) {
        logger.debug("Processing audio chunk: sessionId={}, chunkIndex={}", 
                request.getSessionId(), request.getChunkIndex());

        // Initialize or retrieve session context
        SessionContext context = sessionContexts.computeIfAbsent(
                request.getSessionId(),
                key -> new SessionContext(request.getSessionId()));

        // Add chunk to session buffer
        context.addChunk(request);

        // Check if API key is configured
        if (!isGeminiApiKeyConfigured()) {
            logger.error("Gemini API key is not configured. Set GEMINI_API_KEY environment variable.");
            return Mono.error(new RuntimeException("Gemini API key is not configured. Please set GEMINI_API_KEY environment variable."));
        }

        // Send to Gemini API
        return geminiService.transcribeAudioChunk(request.getAudioData(), getMimeType(request.getFormat()))
                .map(geminiResponse -> convertToTranscriptionResponse(geminiResponse, request))
                .doOnNext(response -> {
                    logger.debug("Transcription completed for chunk: {}", response.getChunkIndex());
                    if (request.getIsFinal() != null && request.getIsFinal()) {
                        cleanupSession(request.getSessionId());
                    }
                })
                .doOnError(error -> {
                    logger.error("Error processing audio chunk: {}", error.getMessage(), error);
                    if (request.getIsFinal() != null && request.getIsFinal()) {
                        cleanupSession(request.getSessionId());
                    }
                });
    }

    /**
     * Process multiple audio chunks as a stream.
     */
    public Flux<TranscriptionResponse> processAudioStream(Flux<AudioChunkRequest> audioChunks) {
        return audioChunks
                .flatMap(this::processAudioChunk)
                .onErrorResume(error -> {
                    logger.error("Stream error: {}", error.getMessage(), error);
                    return Mono.empty();
                });
    }

    /**
     * Get session context for a specific session ID.
     */
    public SessionContext getSessionContext(String sessionId) {
        return sessionContexts.get(sessionId);
    }

    /**
     * Clean up session resources.
     */
    private void cleanupSession(String sessionId) {
        SessionContext context = sessionContexts.remove(sessionId);
        if (context != null) {
            logger.info("Session cleaned up: {}", sessionId);
        }
    }

    /**
     * Convert Gemini API response to TranscriptionResponse.
     */
    private TranscriptionResponse convertToTranscriptionResponse(
            GeminiResponse geminiResponse,
            AudioChunkRequest request) {
        
        String transcribedText = "";
        if (geminiResponse.getCandidates() != null && !geminiResponse.getCandidates().isEmpty()) {
            GeminiResponse.Candidate candidate = geminiResponse.getCandidates().get(0);
            if (candidate.getContent() != null && candidate.getContent().getParts() != null) {
                transcribedText = candidate.getContent().getParts().stream()
                        .map(GeminiResponse.Part::getText)
                        .reduce("", (a, b) -> a + b);
            }
        }

        return TranscriptionResponse.builder()
                .sessionId(request.getSessionId())
                .text(transcribedText)
                .chunkIndex(request.getChunkIndex())
                .isFinal(request.getIsFinal() != null && request.getIsFinal())
                .timestamp(System.currentTimeMillis())
                .confidence(0.95) // Gemini doesn't provide confidence, use default
                .language("en") // Would need to detect from Gemini response in real scenario
                .build();
    }

    /**
     * Get MIME type for audio format.
     */
    private String getMimeType(String format) {
        if (format == null) {
            return "audio/wav";
        }
        return switch (format.toLowerCase()) {
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "webm" -> "audio/webm";
            case "ogg" -> "audio/ogg";
            case "pcm" -> "audio/raw";
            default -> "audio/wav";
        };
    }

    /**
     * Check if Gemini API key is properly configured.
     */
    private boolean isGeminiApiKeyConfigured() {
        String apiKey = geminiService.getApiKey();
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("your-api-key-here");
    }

    /**
     * Inner class to maintain session state.
     */
    public static class SessionContext {
        private final String sessionId;
        private final long createdAt;
        private long lastActivityAt;
        private int totalChunksReceived = 0;
        private int totalChunksProcessed = 0;

        public SessionContext(String sessionId) {
            this.sessionId = sessionId;
            this.createdAt = System.currentTimeMillis();
            this.lastActivityAt = createdAt;
        }

        public void addChunk(AudioChunkRequest request) {
            this.lastActivityAt = System.currentTimeMillis();
            this.totalChunksReceived++;
        }

        public void markChunkProcessed() {
            this.totalChunksProcessed++;
        }

        // Getters
        public String getSessionId() { return sessionId; }
        public long getCreatedAt() { return createdAt; }
        public long getLastActivityAt() { return lastActivityAt; }
        public int getTotalChunksReceived() { return totalChunksReceived; }
        public int getTotalChunksProcessed() { return totalChunksProcessed; }
    }
}

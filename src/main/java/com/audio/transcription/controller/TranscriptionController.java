package com.audio.transcription.controller;

import com.audio.transcription.dto.AudioChunkRequest;
import com.audio.transcription.dto.TranscriptionResponse;
import com.audio.transcription.service.AudioProcessingService;
import com.audio.transcription.service.GeminiTranscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST Controller for transcription endpoints.
 * Provides HTTP endpoints for audio processing and transcription.
 */
@RestController
@RequestMapping("/transcribe")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TranscriptionController {

    private static final Logger logger = LoggerFactory.getLogger(TranscriptionController.class);

    private final AudioProcessingService audioProcessingService;
    private final GeminiTranscriptionService geminiTranscriptionService;

    public TranscriptionController(
            AudioProcessingService audioProcessingService,
            GeminiTranscriptionService geminiTranscriptionService) {
        this.audioProcessingService = audioProcessingService;
        this.geminiTranscriptionService = geminiTranscriptionService;
    }

    /**
     * Process a single audio chunk via HTTP.
     * POST /api/transcribe/chunk
     */
    @PostMapping("/chunk")
    public Mono<ResponseEntity<TranscriptionResponse>> transcribeChunk(
            @RequestBody AudioChunkRequest request) {
        logger.info("Received transcription request for session: {}", request.getSessionId());

        return audioProcessingService.processAudioChunk(request)
                .map(response -> ResponseEntity.ok(response))
                .onErrorResume(error -> {
                    logger.error("Error processing chunk", error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    /**
     * Health check endpoint.
     * GET /api/transcribe/health
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<String>> healthCheck() {
        return geminiTranscriptionService.healthCheck()
                .map(isHealthy -> {
                    if (isHealthy) {
                        return ResponseEntity.ok("Gemini API is healthy");
                    } else {
                        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                                .body("Gemini API is unavailable");
                    }
                })
                .onErrorResume(error -> 
                    Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                            .body("Health check failed: " + error.getMessage())));
    }

    /**
     * Get status of a transcription session.
     * GET /api/transcribe/session/{sessionId}/status
     */
    @GetMapping("/session/{sessionId}/status")
    public ResponseEntity<SessionStatus> getSessionStatus(@PathVariable String sessionId) {
        AudioProcessingService.SessionContext context = audioProcessingService.getSessionContext(sessionId);
        
        if (context == null) {
            return ResponseEntity.notFound().build();
        }

        SessionStatus status = SessionStatus.builder()
                .sessionId(sessionId)
                .createdAt(context.getCreatedAt())
                .lastActivityAt(context.getLastActivityAt())
                .totalChunksReceived(context.getTotalChunksReceived())
                .totalChunksProcessed(context.getTotalChunksProcessed())
                .build();

        return ResponseEntity.ok(status);
    }

    /**
     * Session status DTO.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class SessionStatus {
        private String sessionId;
        private long createdAt;
        private long lastActivityAt;
        private int totalChunksReceived;
        private int totalChunksProcessed;
    }
}

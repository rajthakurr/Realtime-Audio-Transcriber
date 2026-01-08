package com.audio.transcription.handler;

import com.audio.transcription.dto.AudioChunkRequest;
import com.audio.transcription.dto.TranscriptionResponse;
import com.audio.transcription.dto.WebSocketMessage;
import com.audio.transcription.service.AudioProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for bidirectional audio streaming and real-time transcription.
 * Handles audio chunks, processes them through Gemini API, and streams back transcriptions.
 */
@Component
public class AudioStreamWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(AudioStreamWebSocketHandler.class);

    private final AudioProcessingService audioProcessingService;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    public AudioStreamWebSocketHandler(
            AudioProcessingService audioProcessingService,
            ObjectMapper objectMapper) {
        this.audioProcessingService = audioProcessingService;
        this.objectMapper = objectMapper;
    }

    /**
     * Handle WebSocket connection.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = generateSessionId();
        session.getAttributes().put("sessionId", sessionId);
        activeSessions.put(sessionId, session);
        logger.info("WebSocket connection established: sessionId={}", sessionId);

        // Send welcome message
        sendMessage(session, createStatusMessage(sessionId, "connected", "WebSocket connected. Ready to receive audio."));
    }

    /**
     * Handle incoming WebSocket messages (audio chunks).
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        String sessionId = (String) session.getAttributes().get("sessionId");

        logger.debug("Received message from sessionId={}, size={} bytes", sessionId, payload.length());

        try {
            // Parse incoming message
            WebSocketMessage<?> incomingMessage = objectMapper.readValue(payload, 
                    objectMapper.getTypeFactory().constructParametricType(WebSocketMessage.class, Object.class));

            if ("audio_chunk".equals(incomingMessage.getType())) {
                handleAudioChunk(session, sessionId, incomingMessage);
            } else {
                logger.warn("Unknown message type: {}", incomingMessage.getType());
            }
        } catch (Exception e) {
            logger.error("Error processing message from session {}: {}", sessionId, e.getMessage(), e);
            sendErrorMessage(session, "Failed to process message: " + e.getMessage());
        }
    }

    /**
     * Handle audio chunk processing.
     */
    private void handleAudioChunk(WebSocketSession session, String sessionId, WebSocketMessage<?> message) {
        try {
            // Convert payload to AudioChunkRequest
            AudioChunkRequest request = objectMapper.convertValue(message.getPayload(), AudioChunkRequest.class);
            request.setSessionId(sessionId);

            logger.debug("Processing audio chunk: chunkIndex={}, isFinal={}", 
                    request.getChunkIndex(), request.getIsFinal());

            // Process asynchronously using Reactor
            Mono.fromCallable(() -> audioProcessingService.processAudioChunk(request))
                    .flatMap(mono -> mono)
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe(
                            transcriptionResponse -> {
                                try {
                                    sendTranscriptionResponse(session, transcriptionResponse);
                                } catch (IOException e) {
                                    logger.error("Failed to send transcription response", e);
                                }
                            },
                            error -> {
                                logger.error("Error processing audio chunk", error);
                                try {
                                    sendErrorMessage(session, "Transcription error: " + error.getMessage());
                                } catch (IOException e) {
                                    logger.error("Failed to send error message", e);
                                }
                            },
                            () -> logger.debug("Audio chunk processing completed")
                    );
        } catch (Exception e) {
            logger.error("Error handling audio chunk", e);
            try {
                sendErrorMessage(session, "Error: " + e.getMessage());
            } catch (IOException ex) {
                logger.error("Failed to send error message", ex);
            }
        }
    }

    /**
     * Send transcription response back to client.
     */
    private void sendTranscriptionResponse(WebSocketSession session, TranscriptionResponse response) throws IOException {
        WebSocketMessage<TranscriptionResponse> message = WebSocketMessage.<TranscriptionResponse>builder()
                .type("transcription")
                .payload(response)
                .messageId(UUID.randomUUID().toString())
                .timestamp(System.currentTimeMillis())
                .build();

        sendMessage(session, message);
    }

    /**
     * Send status message to client.
     */
    private WebSocketMessage<String> createStatusMessage(String sessionId, String status, String message) {
        return WebSocketMessage.<String>builder()
                .type("status")
                .payload(message)
                .messageId(sessionId + "-" + status)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Send error message to client.
     */
    private void sendErrorMessage(WebSocketSession session, String errorMessage) throws IOException {
        WebSocketMessage<String> message = WebSocketMessage.<String>builder()
                .type("error")
                .payload(errorMessage)
                .messageId(UUID.randomUUID().toString())
                .timestamp(System.currentTimeMillis())
                .build();

        sendMessage(session, message);
    }

    /**
     * Generic method to send any message to client.
     */
    private void sendMessage(WebSocketSession session, Object message) throws IOException {
        if (session.isOpen()) {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
            logger.debug("Message sent to client");
        } else {
            logger.warn("Cannot send message: session is closed");
        }
    }

    /**
     * Handle WebSocket connection closure.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = (String) session.getAttributes().get("sessionId");
        activeSessions.remove(sessionId);
        logger.info("WebSocket connection closed: sessionId={}, status={}", sessionId, status);
    }

    /**
     * Handle errors.
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = (String) session.getAttributes().get("sessionId");
        logger.error("WebSocket transport error for sessionId={}: {}", sessionId, exception.getMessage(), exception);
        
        try {
            sendErrorMessage(session, "Connection error: " + exception.getMessage());
        } catch (IOException e) {
            logger.error("Failed to send error message", e);
        }
    }

    /**
     * Get number of active sessions.
     */
    public int getActiveSessions() {
        return activeSessions.size();
    }

    /**
     * Generate unique session ID.
     */
    private String generateSessionId() {
        return "session-" + UUID.randomUUID().toString();
    }
}

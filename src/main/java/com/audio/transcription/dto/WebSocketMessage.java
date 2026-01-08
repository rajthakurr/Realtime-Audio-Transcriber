package com.audio.transcription.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket message wrapper for audio chunks.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketMessage<T> {
    
    private String type; // "audio_chunk", "transcription", "error", "status"
    private T payload;
    
    @JsonProperty("message_id")
    private String messageId;
    
    private Long timestamp;
}

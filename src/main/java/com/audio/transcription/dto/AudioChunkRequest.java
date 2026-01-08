package com.audio.transcription.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an audio chunk received from the client.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AudioChunkRequest {
    
    @JsonProperty("audio_data")
    private String audioData; // Base64 encoded audio chunk
    
    @JsonProperty("session_id")
    private String sessionId;
    
    @JsonProperty("chunk_index")
    private Long chunkIndex;
    
    @JsonProperty("is_final")
    private Boolean isFinal; // True if this is the last chunk
    
    @JsonProperty("format")
    private String format; // e.g., "wav", "mp3", "pcm"
}

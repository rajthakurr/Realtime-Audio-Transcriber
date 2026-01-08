package com.audio.transcription.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a transcription response from Gemini API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranscriptionResponse {
    
    @JsonProperty("session_id")
    private String sessionId;
    
    @JsonProperty("text")
    private String text; // Transcribed text
    
    @JsonProperty("is_final")
    private Boolean isFinal; // Whether this is the final transcription
    
    @JsonProperty("confidence")
    private Double confidence; // Confidence score (0-1)
    
    @JsonProperty("chunk_index")
    private Long chunkIndex;
    
    @JsonProperty("timestamp")
    private Long timestamp; // Server timestamp in milliseconds
    
    @JsonProperty("language")
    private String language; // Detected language code
}

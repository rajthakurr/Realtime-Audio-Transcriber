package com.audio.transcription.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Gemini API request/response structures for streaming transcription.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeminiRequest {
    
    @JsonProperty("contents")
    private List<Content> contents;
    
    @JsonProperty("generationConfig")
    private GenerationConfig generationConfig;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Content {
        @JsonProperty("parts")
        private List<Part> parts;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Part {
        @JsonProperty("text")
        private String text;
        
        @JsonProperty("inline_data")
        private InlineData inlineData;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InlineData {
        @JsonProperty("mime_type")
        private String mimeType; // e.g., "audio/wav"
        
        @JsonProperty("data")
        private String data; // Base64 encoded audio
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GenerationConfig {
        @JsonProperty("temperature")
        private Double temperature;
        
        @JsonProperty("top_p")
        private Double topP;
        
        @JsonProperty("top_k")
        private Integer topK;
        
        @JsonProperty("max_output_tokens")
        private Integer maxOutputTokens;
    }
}

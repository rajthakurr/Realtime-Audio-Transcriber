package com.audio.transcription.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Gemini API response structure for streaming transcription.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeminiResponse {
    
    @JsonProperty("candidates")
    private List<Candidate> candidates;
    
    @JsonProperty("usageMetadata")
    private UsageMetadata usageMetadata;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Candidate {
        @JsonProperty("content")
        private Content content;
        
        @JsonProperty("finishReason")
        private String finishReason;
        
        @JsonProperty("index")
        private Integer index;
    }
    
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
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UsageMetadata {
        @JsonProperty("input_tokens")
        private Integer inputTokens;
        
        @JsonProperty("output_tokens")
        private Integer outputTokens;
    }
}

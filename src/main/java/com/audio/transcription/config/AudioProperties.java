package com.audio.transcription.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for audio processing.
 */
@Data
@Component
@ConfigurationProperties(prefix = "audio")
public class AudioProperties {
    
    private Chunk chunk = new Chunk();
    private Processing processing = new Processing();
    
    @Data
    public static class Chunk {
        private int size = 4096;
        private String format = "WAV";
    }
    
    @Data
    public static class Processing {
        private int bufferSize = 10;
        private int maxConcurrentSessions = 100;
    }
}

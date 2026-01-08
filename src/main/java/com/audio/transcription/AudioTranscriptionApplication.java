package com.audio.transcription;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main entry point for the Audio Transcription Backend service.
 * Provides real-time streaming transcription using Spring WebFlux and Gemini API.
 */
@SpringBootApplication
@EnableAsync
public class AudioTranscriptionApplication {

    public static void main(String[] args) {
        SpringApplication.run(AudioTranscriptionApplication.class, args);
    }
}

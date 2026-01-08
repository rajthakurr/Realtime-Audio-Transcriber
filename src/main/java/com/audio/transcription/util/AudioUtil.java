package com.audio.transcription.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utility class for audio processing and encoding operations.
 */
public class AudioUtil {

    /**
     * Encode byte array to Base64 string.
     */
    public static String encodeToBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Decode Base64 string to byte array.
     */
    public static byte[] decodeFromBase64(String data) {
        return Base64.getDecoder().decode(data);
    }

    /**
     * Get MIME type for audio format.
     */
    public static String getMimeType(String format) {
        if (format == null) {
            return "audio/wav";
        }
        return switch (format.toLowerCase()) {
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "webm" -> "audio/webm";
            case "ogg" -> "audio/ogg";
            case "pcm" -> "audio/raw";
            case "flac" -> "audio/flac";
            case "m4a" -> "audio/mp4";
            default -> "audio/wav";
        };
    }

    /**
     * Check if format is valid audio format.
     */
    public static boolean isValidAudioFormat(String format) {
        return format != null && (format.equalsIgnoreCase("wav") ||
                format.equalsIgnoreCase("mp3") ||
                format.equalsIgnoreCase("webm") ||
                format.equalsIgnoreCase("ogg") ||
                format.equalsIgnoreCase("pcm") ||
                format.equalsIgnoreCase("flac") ||
                format.equalsIgnoreCase("m4a"));
    }

    /**
     * Validate audio chunk size.
     */
    public static boolean isValidChunkSize(int size, int maxSize) {
        return size > 0 && size <= maxSize;
    }
}

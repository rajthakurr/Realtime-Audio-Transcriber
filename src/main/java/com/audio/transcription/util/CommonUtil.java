package com.audio.transcription.util;

import java.util.UUID;

/**
 * Utility class for common operations.
 */
public class CommonUtil {

    /**
     * Generate unique ID.
     */
    public static String generateUniqueId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate session ID.
     */
    public static String generateSessionId() {
        return "session-" + UUID.randomUUID().toString();
    }

    /**
     * Generate message ID.
     */
    public static String generateMessageId() {
        return "msg-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString();
    }

    /**
     * Check if string is null or empty.
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Format duration in milliseconds to readable string.
     */
    public static String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%d:%02d", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
}

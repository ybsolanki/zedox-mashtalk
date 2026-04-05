package com.zedox.meshtalk.ai;

import android.util.Log;
import com.zedox.meshtalk.utils.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Emergency Detector for MeshTalk
 * Detects emergency keywords in messages across multiple languages
 * Works completely offline – no internet or model download required
 * Team ZEDOX - Imagine Cup 2025
 */
public class EmergencyDetector {

    private static final String TAG = "EmergencyDetector";

    /** Weight applied per matched keyword when computing confidence (capped at 1.0) */
    private static final float CONFIDENCE_MULTIPLIER = 0.35f;

    /**
     * Result of an emergency analysis
     */
    public static class EmergencyResult {
        public final boolean isEmergency;
        public final float confidence;         // 0.0 – 1.0
        public final List<String> matchedKeywords;

        EmergencyResult(boolean isEmergency, float confidence, List<String> matched) {
            this.isEmergency = isEmergency;
            this.confidence = confidence;
            this.matchedKeywords = matched;
        }
    }

    /**
     * Analyse a message and return an {@link EmergencyResult}.
     * Detection is keyword-based and fully offline.
     *
     * @param messageText The raw message text (any language)
     * @return EmergencyResult describing whether the message is an emergency
     */
    public EmergencyResult analyze(String messageText) {
        if (messageText == null || messageText.trim().isEmpty()) {
            return new EmergencyResult(false, 0f, new ArrayList<>());
        }

        String lowerText = messageText.toLowerCase(Locale.ROOT);
        List<String> matched = new ArrayList<>();

        for (String keyword : Constants.EMERGENCY_KEYWORDS) {
            // Use contains() for multi-language keywords that may not use
            // standard ASCII word boundaries.
            if (lowerText.contains(keyword.toLowerCase(Locale.ROOT))) {
                matched.add(keyword);
            }
        }

        if (matched.isEmpty()) {
            return new EmergencyResult(false, 0f, matched);
        }

        // Confidence scales with number of matched keywords, capped at 1.0
        float confidence = Math.min(1.0f, matched.size() * CONFIDENCE_MULTIPLIER);
        Log.w(TAG, "Emergency detected! keywords=" + matched + " confidence=" + confidence);
        return new EmergencyResult(true, confidence, matched);
    }

    /**
     * Convenience method – returns {@code true} if the message is an emergency.
     */
    public boolean isEmergency(String messageText) {
        return analyze(messageText).isEmergency;
    }
}

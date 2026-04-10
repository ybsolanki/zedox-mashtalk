package com.zedox.meshtalk.utils;

/**
 * Application constants for MeshTalk
 * Team ZEDOX - Imagine Cup 2025
 */
public class Constants {
    
    // App Info
    public static final String APP_NAME = "MeshTalk";
    public static final String TEAM_NAME = "ZEDOX";
    public static final String VERSION = "1.0.0";

    // SharedPreferences
    public static final String PREFS_NAME = "MeshTalkPrefs";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_STATUS = "status";
    public static final String KEY_LANGUAGE = "language";
    public static final String KEY_CONTACTS = "contacts";

    // QR Code
    public static final String QR_PREFIX = "MESHTALK:";
    
    // Network
    public static final int SERVER_PORT = 8888;
    public static final int CONNECTION_TIMEOUT = 10000;
    public static final int SOCKET_TIMEOUT = 5000;
    
    // Message Types
    public static final String MESSAGE_TYPE_TEXT = "TEXT";
    public static final String MESSAGE_TYPE_EMERGENCY = "EMERGENCY";
    public static final String MESSAGE_TYPE_BROADCAST = "BROADCAST";

    // Voice Call
    /** Dedicated TCP port for peer-to-peer audio streaming */
    public static final int CALL_PORT = 8889;

    // Local broadcast actions (within the same process, via LocalBroadcastManager)
    /** IncomingCallActivity → ChatActivity: user accepted the incoming call */
    public static final String ACTION_CALL_ACCEPT = "com.zedox.meshtalk.ACTION_CALL_ACCEPT";
    /** IncomingCallActivity → ChatActivity: user rejected the incoming call */
    public static final String ACTION_CALL_REJECT = "com.zedox.meshtalk.ACTION_CALL_REJECT";
    /** InCallActivity → ChatActivity: local user hung up */
    public static final String ACTION_CALL_END_LOCAL = "com.zedox.meshtalk.ACTION_CALL_END_LOCAL";
    /** ChatActivity → InCallActivity: the remote peer ended the call */
    public static final String ACTION_PEER_CALL_END = "com.zedox.meshtalk.ACTION_PEER_CALL_END";
    /** ChatActivity → InCallActivity: the remote peer accepted the call (caller side) */
    public static final String ACTION_CALL_ACCEPTED_BY_PEER = "com.zedox.meshtalk.ACTION_CALL_ACCEPTED_BY_PEER";
    
    // Emergency Keywords (Multi-language)
    public static final String[] EMERGENCY_KEYWORDS = {
        // English
        "help", "emergency", "urgent", "911", "fire", "injured",
        // Spanish
        "ayuda", "emergencia", "urgente", "fuego",
        // Hindi
        "मदद", "आपातकाल",
        // Chinese
        "救命", "紧急",
        // Arabic
        "مساعدة", "طوارئ"
    };
    
    // Routing
    public static final int MAX_HOP_COUNT = 10;
    public static final int ROUTE_TIMEOUT = 30000;
    public static final int MAX_RETRY_ATTEMPTS = 3;
    
    // Translation
    public static final int MAX_MESSAGE_LENGTH = 500;
    public static final String DEFAULT_LANGUAGE = "en";
    
    // Supported Languages
    public static final String[] SUPPORTED_LANGUAGES = {
        "en", "es", "hi", "zh", "ar", "fr", "pt", "ru"
    };
    
    // Private constructor
    private Constants() {
        throw new AssertionError("Cannot instantiate Constants");
    }
}

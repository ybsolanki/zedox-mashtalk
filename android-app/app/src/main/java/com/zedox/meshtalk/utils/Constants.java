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
    
    // Network
    public static final int SERVER_PORT = 8888;
    public static final int CONNECTION_TIMEOUT = 10000;
    public static final int SOCKET_TIMEOUT = 5000;
    
    // Message Types
    public static final String MESSAGE_TYPE_TEXT = "TEXT";
    public static final String MESSAGE_TYPE_EMERGENCY = "EMERGENCY";
    public static final String MESSAGE_TYPE_BROADCAST = "BROADCAST";
    
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

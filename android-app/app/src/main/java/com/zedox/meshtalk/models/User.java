package com.zedox.meshtalk.models;

/**
 * User data model for MeshTalk
 * Team ZEDOX - Imagine Cup 2025
 */
public class User {
    
    private String userId;
    private String userName;
    private String deviceId;
    private String preferredLanguage;
    private boolean isOnline;
    private long lastSeen;
    
    // Constructor
    public User(String userId, String userName, String deviceId, String language) {
        this.userId = userId;
        this.userName = userName;
        this.deviceId = deviceId;
        this.preferredLanguage = language;
        this.isOnline = false;
        this.lastSeen = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String lang) { this.preferredLanguage = lang; }
    
    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }
    
    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }
    
    public void updateLastSeen() {
        this.lastSeen = System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", name='" + userName + '\'' +
                ", language='" + preferredLanguage + '\'' +
                ", online=" + isOnline +
                '}';
    }
}

package com.zedox.meshtalk.models;

/**
 * Device data model for MeshTalk
 * Team ZEDOX - Imagine Cup 2025
 */
public class Device {
    
    private String deviceId;
    private String deviceName;
    private String macAddress;
    private String ipAddress;
    private int signalStrength;
    private int batteryLevel;
    private boolean isConnected;
    private boolean isGroupOwner;
    private long lastSeen;
    private int hopDistance;
    
    // Constructor
    public Device(String deviceId, String deviceName, String macAddress) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.macAddress = macAddress;
        this.isConnected = false;
        this.isGroupOwner = false;
        this.signalStrength = 0;
        this.batteryLevel = 100;
        this.lastSeen = System.currentTimeMillis();
        this.hopDistance = 0;
    }
    
    // Getters and Setters
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String name) { this.deviceName = name; }
    
    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String mac) { this.macAddress = mac; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ip) { this.ipAddress = ip; }
    
    public int getSignalStrength() { return signalStrength; }
    public void setSignalStrength(int signal) { this.signalStrength = signal; }
    
    public int getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(int battery) { this.batteryLevel = battery; }
    
    public boolean isConnected() { return isConnected; }
    public void setConnected(boolean connected) { isConnected = connected; }
    
    public boolean isGroupOwner() { return isGroupOwner; }
    public void setGroupOwner(boolean owner) { isGroupOwner = owner; }
    
    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }
    public void updateLastSeen() { this.lastSeen = System.currentTimeMillis(); }
    
    public int getHopDistance() { return hopDistance; }
    public void setHopDistance(int hops) { this.hopDistance = hops; }
    
    /**
     * Check if device is healthy for routing
     */
    public boolean isHealthy() {
        return isConnected && 
               signalStrength > 30 && 
               batteryLevel > 15 &&
               (System.currentTimeMillis() - lastSeen) < 60000;
    }
    
    @Override
    public String toString() {
        return "Device{" +
                "id='" + deviceId + '\'' +
                ", name='" + deviceName + '\'' +
                ", connected=" + isConnected +
                ", signal=" + signalStrength +
                ", battery=" + batteryLevel +
                '}';
    }
}

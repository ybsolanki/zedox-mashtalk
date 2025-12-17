package com.zedox.meshtalk.models;

/**
 * Message data model for MeshTalk
 * Team ZEDOX - Imagine Cup 2025
 */
public class Message {
    
    private String messageId;
    private String senderId;
    private String senderName;
    private String receiverId;
    private String content;
    private String originalLanguage;
    private String translatedContent;
    private String messageType;
    private long timestamp;
    private int hopCount;
    private boolean isEmergency;
    private boolean isSent;
    private boolean isDelivered;
    
    // Constructor
    public Message(String messageId, String senderId, String senderName, 
                   String content, String messageType) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.messageType = messageType;
        this.timestamp = System.currentTimeMillis();
        this.hopCount = 0;
        this.isEmergency = false;
        this.isSent = false;
        this.isDelivered = false;
    }
    
    // Getters and Setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    
    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getOriginalLanguage() { return originalLanguage; }
    public void setOriginalLanguage(String lang) { this.originalLanguage = lang; }
    
    public String getTranslatedContent() { return translatedContent; }
    public void setTranslatedContent(String content) { this.translatedContent = content; }
    
    public String getMessageType() { return messageType; }
    public void setMessageType(String type) { this.messageType = type; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public int getHopCount() { return hopCount; }
    public void setHopCount(int count) { this.hopCount = count; }
    public void incrementHopCount() { this.hopCount++; }
    
    public boolean isEmergency() { return isEmergency; }
    public void setEmergency(boolean emergency) { isEmergency = emergency; }
    
    public boolean isSent() { return isSent; }
    public void setSent(boolean sent) { isSent = sent; }
    
    public boolean isDelivered() { return isDelivered; }
    public void setDelivered(boolean delivered) { isDelivered = delivered; }
    
    @Override
    public String toString() {
        return "Message{" +
                "id='" + messageId + '\'' +
                ", from='" + senderName + '\'' +
                ", content='" + content + '\'' +
                ", emergency=" + isEmergency +
                ", hops=" + hopCount +
                '}';
    }
}

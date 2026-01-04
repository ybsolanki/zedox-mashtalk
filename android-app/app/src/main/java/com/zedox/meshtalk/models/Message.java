package com.zedox.meshtalk.models;

/**
 * Message Model for MeshTalk
 * Represents a single chat message
 * Team ZEDOX - Imagine Cup 2025
 */
public class Message {
    
    private String messageId;
    private String senderId;
    private String senderName;
    private String receiverId;
    private String messageText;
    private long timestamp;
    private boolean isSent;
    private boolean isDelivered;
    private boolean isRead;
    private MessageType type;
    
    public enum MessageType {
        TEXT,
        IMAGE,
        VOICE,
        FILE
    }
    
    // Constructor for text messages
    public Message(String senderId, String senderName, String receiverId, String messageText) {
        this.messageId = generateMessageId();
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.messageText = messageText;
        this.timestamp = System.currentTimeMillis();
        this.isSent = false;
        this.isDelivered = false;
        this.isRead = false;
        this.type = MessageType.TEXT;
    }
    
    // Full constructor
    public Message(String messageId, String senderId, String senderName, String receiverId, 
                   String messageText, long timestamp, boolean isSent, boolean isDelivered, 
                   boolean isRead, MessageType type) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.messageText = messageText;
        this.timestamp = timestamp;
        this.isSent = isSent;
        this.isDelivered = isDelivered;
        this.isRead = isRead;
        this.type = type;
    }
    
    // Generate unique message ID
    private String generateMessageId() {
        return "MSG_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
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
    
    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public boolean isSent() { return isSent; }
    public void setSent(boolean sent) { isSent = sent; }
    
    public boolean isDelivered() { return isDelivered; }
    public void setDelivered(boolean delivered) { isDelivered = delivered; }
    
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
}
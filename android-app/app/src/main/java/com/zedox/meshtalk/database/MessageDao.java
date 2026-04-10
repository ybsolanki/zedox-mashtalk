package com.zedox.meshtalk.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.zedox.meshtalk.models.Message;
import java.util.List;

/**
 * Data Access Object for Messages
 * Provides CRUD operations for the messages table
 * Team ZEDOX - Imagine Cup 2025
 */
@Dao
public interface MessageDao {

    /** Insert a new message; replaces on conflict */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessage(Message message);

    /** Update an existing message (e.g. delivery/read status) */
    @Update
    void updateMessage(Message message);

    /** Get all messages ordered oldest-first */
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    List<Message> getAllMessages();

    /**
     * Get all messages in a conversation between two users,
     * ordered oldest-first.
     */
    @Query("SELECT * FROM messages " +
           "WHERE (senderId = :userId AND receiverId = :contactId) " +
           "   OR (senderId = :contactId AND receiverId = :userId) " +
           "ORDER BY timestamp ASC")
    List<Message> getConversation(String userId, String contactId);

    /** Get unread received messages for a user */
    @Query("SELECT * FROM messages WHERE receiverId = :userId AND isRead = 0")
    List<Message> getUnreadMessages(String userId);

    /** Count unread received messages for a user (used for the home-screen badge) */
    @Query("SELECT COUNT(*) FROM messages WHERE receiverId = :userId AND isRead = 0")
    int countUnreadMessages(String userId);

    /** Mark all messages in a conversation as read */
    @Query("UPDATE messages SET isRead = 1 " +
           "WHERE senderId = :contactId AND receiverId = :userId")
    void markConversationRead(String userId, String contactId);

    /** Delete all messages (for testing / data clear) */
    @Query("DELETE FROM messages")
    void deleteAllMessages();
}

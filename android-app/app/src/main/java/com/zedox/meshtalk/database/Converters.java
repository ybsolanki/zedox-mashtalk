package com.zedox.meshtalk.database;

import androidx.room.TypeConverter;
import com.zedox.meshtalk.models.Message;

/**
 * Room TypeConverters for MeshTalk database
 * Converts non-primitive types to/from storable primitives
 * Team ZEDOX - Imagine Cup 2025
 */
public class Converters {

    @TypeConverter
    public static String fromMessageType(Message.MessageType type) {
        return type == null ? null : type.name();
    }

    @TypeConverter
    public static Message.MessageType toMessageType(String value) {
        return value == null ? null : Message.MessageType.valueOf(value);
    }
}

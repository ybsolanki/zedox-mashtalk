package com.zedox.meshtalk.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.zedox.meshtalk.models.Message;

/**
 * Room Database for MeshTalk
 * Persists messages locally on the device
 * Team ZEDOX - Imagine Cup 2025
 */
@Database(entities = {Message.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract MessageDao messageDao();

    private static volatile AppDatabase instance;

    /**
     * Get singleton database instance
     */
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "meshtalk_database"
                    ).build();
                }
            }
        }
        return instance;
    }
}

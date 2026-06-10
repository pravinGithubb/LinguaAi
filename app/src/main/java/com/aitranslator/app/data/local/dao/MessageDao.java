package com.aitranslator.app.data.local.dao;
import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.aitranslator.app.data.local.entity.ConversationMessage;
import java.util.List;

@Dao
public interface MessageDao {
    @Insert long insert(ConversationMessage message);
    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    LiveData<List<ConversationMessage>> getMessagesBySession(String sessionId);
    @Query("SELECT DISTINCT sessionId FROM messages ORDER BY timestamp DESC")
    LiveData<List<String>> getAllSessionIds();
    @Query("SELECT COUNT(*) FROM messages WHERE sender = 'user'")
    int getTotalUserMessages();
}
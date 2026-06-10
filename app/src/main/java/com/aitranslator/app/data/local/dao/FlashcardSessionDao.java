package com.aitranslator.app.data.local.dao;
import androidx.room.*;
import com.aitranslator.app.data.local.entity.FlashcardSession;
import java.util.List;

@Dao
public interface FlashcardSessionDao {
    @Insert
    long insert(FlashcardSession session);

    @Query("SELECT * FROM flashcard_sessions ORDER BY timestamp DESC LIMIT 10")
    List<FlashcardSession> getRecentSessions();

    @Query("SELECT SUM(knownCount) FROM flashcard_sessions")
    int getTotalKnown();
}
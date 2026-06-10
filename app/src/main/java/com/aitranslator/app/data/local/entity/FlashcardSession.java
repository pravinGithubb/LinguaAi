package com.aitranslator.app.data.local.entity;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "flashcard_sessions")
public class FlashcardSession {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long timestamp;
    public int totalCards;
    public int knownCount;
    public int unknownCount;
    public int durationSeconds;

    public FlashcardSession(long timestamp, int totalCards, int knownCount,
            int unknownCount, int durationSeconds) {
        this.timestamp = timestamp; this.totalCards = totalCards;
        this.knownCount = knownCount; this.unknownCount = unknownCount;
        this.durationSeconds = durationSeconds;
    }
}
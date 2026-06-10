package com.aitranslator.app.data.local.entity;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class ConversationMessage {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String sessionId;
    public String sender; // "user" or "ai"
    public String content;
    public String translation;
    public long timestamp;
    public String fromLanguage;
    public String toLanguage;

    public ConversationMessage(String sessionId, String sender, String content,
            String translation, long timestamp, String fromLanguage, String toLanguage) {
        this.sessionId = sessionId; this.sender = sender; this.content = content;
        this.translation = translation; this.timestamp = timestamp;
        this.fromLanguage = fromLanguage; this.toLanguage = toLanguage;
    }
}
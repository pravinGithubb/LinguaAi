package com.aitranslator.app.data.local.entity;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "translation_history")
public class TranslationHistory {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String originalText;
    public String translatedText;
    public String fromLanguage;
    public String toLanguage;
    public long timestamp;
    public boolean isFavorite;

    public TranslationHistory(String originalText, String translatedText,
            String fromLanguage, String toLanguage, long timestamp) {
        this.originalText = originalText; this.translatedText = translatedText;
        this.fromLanguage = fromLanguage; this.toLanguage = toLanguage;
        this.timestamp = timestamp; this.isFavorite = false;
    }
}
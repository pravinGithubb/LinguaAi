package com.aitranslator.app.data.local.entity;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "phrases")
public class Phrase {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String category;       // "Greetings", "Food", "Emergency", etc.
    public String phraseTarget;   // phrase in target language
    public String phraseNative;   // translation in user's native language
    public String phonetic;       // romanization / IPA, may be empty
    public String languageCode;   // target language code, e.g. "es"
    public boolean isFavorite;
    public long dateAdded;

    public Phrase(String category, String phraseTarget, String phraseNative,
                  String phonetic, String languageCode) {
        this.category = category;
        this.phraseTarget = phraseTarget;
        this.phraseNative = phraseNative;
        this.phonetic = phonetic;
        this.languageCode = languageCode;
        this.isFavorite = false;
        this.dateAdded = System.currentTimeMillis();
    }
}
